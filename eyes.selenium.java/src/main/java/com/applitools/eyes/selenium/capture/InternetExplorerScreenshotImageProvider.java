package com.applitools.eyes.selenium.capture;

import com.applitools.eyes.*;
import com.applitools.eyes.capture.ImageProvider;
import com.applitools.eyes.positioning.PositionProvider;
import com.applitools.eyes.selenium.SeleniumEyes;
import com.applitools.eyes.selenium.SeleniumJavaScriptExecutor;
import com.applitools.eyes.selenium.frames.FrameChain;
import com.applitools.eyes.selenium.positioning.ScrollPositionProviderFactory;
import com.applitools.eyes.selenium.wrappers.EyesWebDriver;
import com.applitools.utils.ImageUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.awt.image.BufferedImage;

public class InternetExplorerScreenshotImageProvider implements ImageProvider {

    private final SeleniumEyes eyes;
    private final Logger logger;
    private final TakesScreenshot tsInstance;
    private final IEyesJsExecutor jsExecutor;
    private final UserAgent userAgent;

    public InternetExplorerScreenshotImageProvider(SeleniumEyes eyes, Logger logger, TakesScreenshot tsInstance, UserAgent userAgent) {
        this.eyes = eyes;
        this.logger = logger;
        this.tsInstance = tsInstance;
        this.jsExecutor = new SeleniumJavaScriptExecutor((EyesWebDriver) eyes.getDriver());
        this.userAgent = userAgent;
    }

    public BufferedImage getImage() {
        logger.verbose("Getting screenshot as base64...");
        String screenshot64 = tsInstance.getScreenshotAs(OutputType.BASE64);
        logger.verbose("Done getting base64! Creating BufferedImage...");
        BufferedImage image = ImageUtils.imageFromBase64(screenshot64);

        double scaleRatio = eyes.getDevicePixelRatio();
        RectangleSize originalViewportSize = eyes.getViewportSize();
        RectangleSize viewportSize = originalViewportSize.scale(scaleRatio);

        if (image.getHeight() > viewportSize.getHeight() || image.getWidth() > viewportSize.getWidth()) {
            //Damn IE driver returns full page screenshot even when not asked to!
            logger.verbose("seems IE returned full page screenshot rather than only the viewport.");
            eyes.getDebugScreenshotsProvider().save(image, "IE");
            if (eyes.getIsCutProviderExplicitlySet()) {
                return image;
            }

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
        return image;
    }

}
