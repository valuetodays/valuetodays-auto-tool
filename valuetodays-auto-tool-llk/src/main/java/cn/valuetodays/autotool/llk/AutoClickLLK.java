package cn.valuetodays.autotool.llk;

import cn.valuetodays.autotool.common.win32.ScreenUtils;
import cn.valuetodays.autotool.common.win32.Win32Utils;
import com.sun.jna.platform.win32.WinDef;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * .
 *
 * @author lei.liu
 * @since 2023-05-27 21:42
 */
public class AutoClickLLK {
    private WinDef.HWND hwnd;
    private GameMode mode;
    private StringTile[][] tileMap;

    public static WinDef.HWND getHwnd(String windowTitle) {
        return Win32Utils.USER_32.FindWindow(null, windowTitle);
    }

    /**
     * 自动点击（外部调用入口）
     */
    public void doAutoClick() throws IOException {
        String basePath = "X:/llk";
        System.out.println("doAutoClick() begin");

        String windowTitle = ("连连看 v4.95\t");
        hwnd = getHwnd(windowTitle);
        if (Objects.isNull(hwnd)) {
            System.out.println("游戏未运行");
            return;
        }

        Win32Utils.USER_32.SetForegroundWindow(hwnd);
        Win32Utils.USER_32_PLUS.SetActiveWindow(hwnd);
        Win32Utils.USER_32.BringWindowToTop(hwnd);

        mode = GameMode.CHUJI;

        WinDef.RECT clientRect = new WinDef.RECT();
        Win32Utils.USER_32.GetClientRect(hwnd, clientRect);
        System.out.println("clientRect: " + clientRect);
        WinDef.RECT windowRect = new WinDef.RECT();
        Win32Utils.USER_32.GetWindowRect(hwnd, windowRect);
        System.out.println("windowRect: " + windowRect);
        BufferedImage ss = ScreenUtils.getScreenshotOfWindow(hwnd, false, 1.0f);
        File fullFile = new File(basePath + "/s0-full.jpg");
//        ScreenUtils.saveScreenshotToJpgFile(ss, fullFile);

        List<String> imageIdList = new ArrayList<>();
        int left = mode.getLeft();
        int top = mode.getTop();
        int tileWidth = mode.getTileWidth();
        int tileHeight = mode.getTileHeight();
        int horizontalTileCount = mode.getHorizontalTileCount();
        int verticalTileCount = mode.getVerticalTileCount();
        // 加上四周边界
        tileMap = new StringTile[verticalTileCount + 2][horizontalTileCount + 2];
        for (int i = 0; i < tileMap.length; i++) {
            Arrays.fill(tileMap[i], StringTile.BORDER);
        }

        for (int i = 0; i < horizontalTileCount; i++) {
            for (int j = 0; j < verticalTileCount; j++) {
                BufferedImage subimage = ss.getSubimage(
                    left + i * tileWidth + 2, top + j * tileHeight + 2,
                    tileWidth - 4, tileHeight - 4
                );
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
                    ScreenUtils.saveScreenshotToJpgFile(subimage, baos);
                    String md5Hex = DigestUtils.md5Hex(baos.toByteArray());
                    tileMap[j + 1][i + 1] = new StringTile(md5Hex);
                    File subFile = new File(basePath + "/ss-" + md5Hex + i + "_" + j + ".jpg");
                    // 测试时可以生成子图片
//                    ScreenUtils.saveScreenshotToJpgFile(subimage, subFile);
                    imageIdList.add(md5Hex);
                }
            }
        }
        HashSet<String> distinctImageIdList = new HashSet<>(imageIdList);
        int tileCount = mode.getHorizontalTileCount() * mode.getVerticalTileCount(); // 图块数量
        int exceptedTileCategoryCount = tileCount / mode.getTileCountPerCategory(); // 图块种类类数量
        System.out.println("!! distinct: " + distinctImageIdList.size() + ", total: " + imageIdList.size());
        if (exceptedTileCategoryCount != distinctImageIdList.size()) {
            System.err.println("解析tile图片失败，有误识别的图片，建议重新开始一局。");
        }
        printTileMap();

        for (int i = 0; i < imageIdList.size() / 2; i++) {
            autoClick();
            System.out.println("i end " + i);
        }

        System.out.println("doAutoClick() end");

    }

    /**
     * 自动点击
     */
    private void autoClick() {
        // 边界不处理了
        for (int point1x = 1; point1x < tileMap.length; point1x++) {
            for (int point1y = 1; point1y < tileMap[0].length; point1y++) {
                tryClickWithPoint1(tileMap, point1x, point1y);
            }
        }

    }

    /**
     * 读取二维数组中的值
     * （注意：不要使用之前获取的值，因为当消除相同图块后，这两个图块的值都已被清空）
     */
    private StringTile readTile(int x, int y) {
        return tileMap[x][y];
    }

    /**
     * 尝试以point1为基础，点击另一个相同的图块
     */
    private void tryClickWithPoint1(StringTile[][] tileMap, int point1x, int point1y) {
        StringTile point1Tile = readTile(point1x, point1y);
        if (point1Tile.isCleared()) {
            return;
        }

        Point point1 = new Point(point1x, point1y);
        for (int point2x = 1; point2x < tileMap.length; point2x++) {
            for (int point2y = 1; point2y < tileMap[0].length; point2y++) {
                StringTile point2Tile = tileMap[point2x][point2y];
                if (point2Tile.isCleared()
                    || (point1x == point2x && point1y == point2y)
                    || !point1Tile.isEquals(point2Tile)) {
                    continue;
                }
                Point point2 = new Point(point2x, point2y);
                List<Point> list = LLKCore.checkLinked(tileMap, point1, point2);
                System.out.println("try click: "
                    + "(x=" + point1.x + ",y=" + point1.y + "), "
                    + "(x=" + point2.x + ",y=" + point2.y + ")"
                );
                if (CollectionUtils.isNotEmpty(list)) {
                    System.out.println("clicked: "
                        + "(x=" + point1.x + ",y=" + point1.y + "), "
                        + "(x=" + point2.x + ",y=" + point2.y + ")"
                    );
                    clickTilePair( point1.y-1, point1.x-1, point2.y-1, point2.x-1);
                    clientTileValue(point1.x, point1.y, point2.x, point2.y);
                    printTileMap();
                    Win32Utils.sleep(100);
                    // 如果point1已清空，就开始新的循环
                    if (readTile(point1x, point1y).isCleared()) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * 打印图块二维数组，用于调试
     */
    private void printTileMap() {
        // print
        for (int i = 0; i < tileMap.length; i++) {
            for (int j = 0; j < tileMap[0].length; j++) {
                System.out.print(tileMap[i][j].getStatus() + " ");
            }
            System.out.println();
        }
    }

    /**
     * 消除图块二维数组中的值（用于点击两个相同的图块后）
     */
    private void clientTileValue(int x, int y, int x1, int y1) {
        tileMap[x][y] = StringTile.CLEARED;
        tileMap[x1][y1] = StringTile.CLEARED;
    }

    /**
     * 点击一对图块
     */
    private void clickTilePair(int title1X, int title1Y, int title2X, int title2Y) {
        clickTile(title1X, title1Y);
        clickTile(title2X, title2Y);
    }

    /**
     * 点击图块
     * @param titleX x
     * @param titleY y
     */
    private void clickTile(int titleX, int titleY) {
        int left = mode.getLeft();
        int top = mode.getTop();
        int tileWidth = mode.getTileWidth();
        int tileHeight = mode.getTileHeight();
        int x = left + titleX * tileWidth + tileWidth / 2;
        int y = top + titleY * tileHeight + tileHeight / 2;
        Win32Utils.clickWindow(hwnd, x, y);
    }

}
