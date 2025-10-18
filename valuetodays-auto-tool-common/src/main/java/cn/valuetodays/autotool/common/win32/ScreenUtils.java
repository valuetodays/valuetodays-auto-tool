package cn.valuetodays.autotool.common.win32;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * 将窗口保存为截图.
 *
 * @author lei.liu
 * @since 2023-05-26 16:17
 */
@Slf4j
public final class ScreenUtils {
    private ScreenUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final Robot robot;

    static {
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            throw new IllegalStateException("not support", ex);
        }
    }

    public static BufferedImage getScreenshotOfWindow(WinDef.HWND hWnd) {
        return getScreenshotOfWindow(hWnd, true, 1.0f);
    }
    public static BufferedImage getScreenshotOfWindowClient(WinDef.HWND hWnd) {
        return getScreenshotOfWindow(hWnd, false, 1.0f);
    }

    /**
     *
     * @param hWnd window handle
     * @param fullWindow false for only client region
     */
    public static BufferedImage getScreenshotOfWindow(WinDef.HWND hWnd, boolean fullWindow, float systemZoom) {
        if (Objects.isNull(robot)) {
            throw new IllegalStateException("not support getScreenshotOfWindow");
        }
        if (Objects.isNull(hWnd)) {
            return null;
        }
        if (fullWindow) {
            WinDef.RECT windowRect = new WinDef.RECT();
            User32.INSTANCE.GetWindowRect(hWnd, windowRect);
            Rectangle rect = windowRect.toRectangle();
            // 修正缩放
            Rectangle zoomedRect = new Rectangle(
                Math.round(rect.x * systemZoom),
                Math.round(rect.y * systemZoom),
                Math.round(rect.width * systemZoom),
                Math.round(rect.height * systemZoom)
            );
            return robot.createScreenCapture(zoomedRect);
        } else {
            WinDef.RECT clientRect = new WinDef.RECT();
            User32.INSTANCE.GetClientRect(hWnd, clientRect);
            WinDef.POINT leftTopPoint = new WinDef.POINT(clientRect.left, clientRect.top);
            // 客户区的坐标要转换成屏幕坐标
            Win32Utils.USER_32_PLUS.ClientToScreen(hWnd, leftTopPoint);
            WinDef.POINT rightBottomPoint = new WinDef.POINT(clientRect.right, clientRect.bottom);
            Win32Utils.USER_32_PLUS.ClientToScreen(hWnd, rightBottomPoint);

            int left = Math.round(leftTopPoint.x / systemZoom);
            int top = Math.round(leftTopPoint.y / systemZoom);
            int width = Math.round((rightBottomPoint.x - leftTopPoint.x) / systemZoom);
            int height = Math.round((rightBottomPoint.y - leftTopPoint.y) / systemZoom);
            Rectangle zoomedRect = new Rectangle(left, top, width, height);
            BufferedImage image = robot.createScreenCapture(zoomedRect);
            BufferedImage zoomedImage = ImageUtils.scaleImage(image, systemZoom);
            return zoomedImage;
        }
    }

    public static void saveScreenshotToFile(WinDef.HWND hWnd, String formatName, File file, boolean fullWindow) {
        BufferedImage image = getScreenshotOfWindow(hWnd, fullWindow, 1.0f);
        saveScreenshotToFile(image, formatName, file);
    }

    public static void saveScreenshotToJpgFile(WinDef.HWND hWnd, File file, boolean fullWindow) {
        saveScreenshotToFile(hWnd, "jpg", file, fullWindow);
    }
    public static void saveScreenshotToJpgFile(WinDef.HWND hWnd, File file) {
        saveScreenshotToFile(hWnd, "jpg", file, true);
    }

    public static void saveScreenshotToPngFile(WinDef.HWND hWnd, File file, boolean fullWindow) {
        saveScreenshotToFile(hWnd, "png", file, fullWindow);
    }
    public static void saveScreenshotToPngFile(WinDef.HWND hWnd, File file) {
        saveScreenshotToFile(hWnd, "png", file, true);
    }

    public static void saveScreenshotToFile(BufferedImage image, String formatName, OutputStream output) {
        if (Objects.isNull(image)) {
            throw new NullPointerException("image should be not null");
        }
        String formatNameToUse = formatName;
        if (StringUtils.isBlank(formatName)) {
            formatNameToUse = "jpg";
        }

        try {
            ImageIO.write(image, formatNameToUse, output);
        } catch (IOException e) {
            log.error("Error writing screenshot", e);
        }
    }

    public static void saveScreenshotToFile(BufferedImage image, String formatName, File file) {
        if (Objects.isNull(image)) {
            throw new NullPointerException("image should be not null");
        }
        if (Objects.isNull(file)) {
            throw new NullPointerException("file should be not null");
        }
        if (file.isDirectory()) {
            throw new IllegalStateException("file should not be Directory");
        }
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            boolean f = parentFile.mkdirs();
            if (f) {
                log.info("directory is created: " + parentFile);
            }
        }
        String formatNameToUse = formatName;
        if (StringUtils.isBlank(formatName)) {
            formatNameToUse = "jpg";
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            saveScreenshotToFile(image, formatNameToUse, fos);
//            ImageIO.write(image, formatNameToUse, file);
        } catch (IOException e) {
            log.error("Error writing screenshot", e);
        }
    }

    public static void saveScreenshotToJpgFile(BufferedImage bufferedImage, File file) {
        saveScreenshotToFile(bufferedImage, "jpg", file);
    }

    public static void saveScreenshotToJpgFile(BufferedImage bufferedImage, OutputStream output) {
        saveScreenshotToFile(bufferedImage, "jpg", output);
    }

    public static void saveScreenshotToPngFile(BufferedImage bufferedImage, File file) {
        saveScreenshotToFile(bufferedImage, "png", file);
    }

}
