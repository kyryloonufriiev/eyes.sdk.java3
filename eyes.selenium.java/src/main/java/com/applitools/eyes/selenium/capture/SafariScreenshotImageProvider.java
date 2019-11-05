package com.applitools.eyes.selenium.capture;

import com.applitools.eyes.*;
import com.applitools.eyes.capture.ImageProvider;
import com.applitools.eyes.positioning.PositionProvider;
import com.applitools.eyes.selenium.EyesSeleniumUtils;
import com.applitools.eyes.selenium.SeleniumEyes;
import com.applitools.eyes.selenium.SeleniumJavaScriptExecutor;
import com.applitools.eyes.selenium.frames.FrameChain;
import com.applitools.eyes.selenium.positioning.ScrollPositionProvider;
import com.applitools.eyes.selenium.positioning.ScrollPositionProviderFactory;
import com.applitools.eyes.selenium.wrappers.EyesWebDriver;
import com.applitools.utils.ImageUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SafariScreenshotImageProvider implements ImageProvider {

    private final SeleniumEyes eyes;
    private final Logger logger;
    private final TakesScreenshot tsInstance;
    private final IEyesJsExecutor jsExecutor;
    private final UserAgent userAgent;

    private static Map<RectangleSize, Region[]> devicesRegions = null;
    private RectangleSize cachedViewportSize;

    public SafariScreenshotImageProvider(SeleniumEyes eyes, Logger logger, TakesScreenshot tsInstance, UserAgent userAgent) {
        this.eyes = eyes;
        this.logger = logger;
        this.tsInstance = tsInstance;
        this.jsExecutor = new SeleniumJavaScriptExecutor((EyesWebDriver) eyes.getDriver());
        this.userAgent = userAgent;
    }

    @Override
    public BufferedImage getImage() {
        logger.verbose("Getting screenshot as base64...");
        String screenshot64 = tsInstance.getScreenshotAs(OutputType.BASE64);
        logger.verbose("Done getting base64! Creating BufferedImage...");
        BufferedImage image = ImageUtils.imageFromBase64(screenshot64);

        eyes.getDebugScreenshotsProvider().save(image, "SAFARI");

        if (eyes.getIsCutProviderExplicitlySet()) {
            return image;
        }

        double scaleRatio = eyes.getDevicePixelRatio();

        if (cachedViewportSize == null) {
            cachedViewportSize = EyesSeleniumUtils.getViewportSize((JavascriptExecutor) eyes.getDriver(), logger);
        }

        RectangleSize originalViewportSize = cachedViewportSize;
        RectangleSize viewportSize = originalViewportSize.scale(scaleRatio);

        logger.verbose("logical viewport size: " + originalViewportSize);

        if (userAgent.getOS().equals(OSNames.IOS)) {
            if (devicesRegions == null) {
                initDeviceRegionsTable();
            }

            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            RectangleSize imageSize = new RectangleSize(imageWidth, imageHeight);

            logger.verbose("physical device pixel size: " + imageWidth + " x " + imageHeight);

            if (devicesRegions.containsKey(imageSize)) {

                Region[] resolutions = devicesRegions.get(imageSize);

                float widthRatio = imageWidth / (float) originalViewportSize.getWidth();
                float height = widthRatio * originalViewportSize.getHeight();

                if (Math.abs(height - imageHeight) > 1.5) {
                    Region bestMatchingRect = resolutions[0];
                    float bestHeightDiff = Math.abs(bestMatchingRect.getHeight() - height);
                    for (int i = 1; i < resolutions.length; ++i) {
                        Region rect = resolutions[i];
                        float heightDiff = Math.abs(rect.getHeight() - height);
                        if (heightDiff < bestHeightDiff) {
                            bestHeightDiff = heightDiff;
                            bestMatchingRect = rect;
                        }
                    }
                    logger.verbose("closest crop rect found: " + bestMatchingRect);
                    image = ImageUtils.cropImage(logger, image, bestMatchingRect);
                } else {
                    logger.verbose("no crop needed. must be using chrome emulator.");
                }
            } else {
                logger.verbose("device not found in list. returning original image.");
            }
        } else {
            boolean forceFullPageScreenshot = eyes.getForceFullPageScreenshot();
            if (!forceFullPageScreenshot) {

                Location loc;
                FrameChain currentFrameChain = ((EyesWebDriver) eyes.getDriver()).getFrameChain();

                if (currentFrameChain.size() == 0) {
                    PositionProvider positionProvider = ScrollPositionProviderFactory.getScrollPositionProvider(userAgent, logger, jsExecutor, eyes.getDriver().findElement(By.tagName("html")));
                    loc = positionProvider.getCurrentPosition();
                } else {
                    loc = currentFrameChain.getDefaultContentScrollPosition();
                }

                loc = loc.scale(scaleRatio);

                image = ImageUtils.cropImage(logger, image, new Region(loc, viewportSize));
            }
        }

        return image;
    }


    private void initDeviceRegionsTable() {
        devicesRegions = new HashMap<>();

        devicesRegions.put(new RectangleSize(1536, 2048), new Region[]{new Region(0, 141, 1536, 1907), new Region(0, 206, 1536, 1842), new Region(0, 129, 1536, 1919), new Region(0, 194, 1536, 1854)});
        devicesRegions.put(new RectangleSize(2048, 1536), new Region[]{new Region(0, 141, 2048, 1395), new Region(0, 206, 2048, 1330), new Region(0, 129, 2048, 1407), new Region(0, 194, 2048, 1342)});

        devicesRegions.put(new RectangleSize(828, 1792), new Region[]{new Region(0, 189, 828, 1436)});
        devicesRegions.put(new RectangleSize(1792, 828), new Region[]{new Region(88, 101, 1616, 685)});

        devicesRegions.put(new RectangleSize(1242, 2688), new Region[]{new Region(0, 283, 1242, 2155)});
        devicesRegions.put(new RectangleSize(2688, 1242), new Region[]{new Region(132, 151, 2424, 1028)});

        devicesRegions.put(new RectangleSize(1125, 2436), new Region[]{new Region(0, 283, 1125, 1903)});
        devicesRegions.put(new RectangleSize(2436, 1125), new Region[]{new Region(132, 151, 2172, 930)});

        devicesRegions.put(new RectangleSize(1242, 2208), new Region[]{new Region(0, 211, 1242, 1863), new Region(0, 193, 1242, 1882)});
        devicesRegions.put(new RectangleSize(2208, 1242), new Region[]{new Region(0, 151, 2208, 1090), new Region(0, 231, 2208, 1010)});

        devicesRegions.put(new RectangleSize(750, 1334), new Region[]{new Region(0, 141, 750, 1104), new Region(0, 129, 750, 1116)});
        devicesRegions.put(new RectangleSize(1334, 750), new Region[]{new Region(0, 101, 1334, 648), new Region(0, 89, 1334, 660)});

        devicesRegions.put(new RectangleSize(640, 1136), new Region[]{new Region(0, 129, 640, 918)});
        devicesRegions.put(new RectangleSize(1136, 640), new Region[]{new Region(0, 89, 1136, 462)});

        devicesRegions.put(new RectangleSize(2048, 2732), new Region[]{new Region(0, 141, 2048, 2591)});
        devicesRegions.put(new RectangleSize(2732, 2048), new Region[]{new Region(0, 141, 2732, 1907)});

        devicesRegions.put(new RectangleSize(1668, 2224), new Region[]{new Region(0, 141, 1668, 2083)});
        devicesRegions.put(new RectangleSize(2224, 1668), new Region[]{new Region(0, 141, 2224, 1527)});
    }
}
