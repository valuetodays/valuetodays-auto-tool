package cn.valuetodays.autotool.common.win32;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageUtils {

    /**
     * 按指定倍数放大或缩小图片。
     *
     * @param source 原始 BufferedImage
     * @param scale  放大倍数，比如 1.5f 表示放大 150%
     * @return 新的放大后的 BufferedImage
     */
    public static BufferedImage scaleImage(BufferedImage source, float scale) {
        if (source == null || scale <= 0) {
            throw new IllegalArgumentException("Invalid source or scale: " + scale);
        }

        int newWidth = Math.round(source.getWidth() * scale);
        int newHeight = Math.round(source.getHeight() * scale);

        BufferedImage scaled = new BufferedImage(newWidth, newHeight, source.getType());
        Graphics2D g2d = scaled.createGraphics();

        // 开启平滑缩放和抗锯齿，提高质量
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(source, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return scaled;
    }

}
