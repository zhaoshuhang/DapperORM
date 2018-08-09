package com.sinux.happy.orm.common;

/**
 * 获取当前目录
 *
 * @author zhaosh
 * @since 2018-04-10
 */
public class RootPathHelper {
    private static int osType = 0;

    public String GetCurrentDir(String subDirPath) {
        if (osType == 0) {
            osType = (System.getProperty("os.name").toLowerCase().startsWith("win")) ? 1 : 2;
        }

        if (osType == 1) {
            String s = this.getClass().getResource("/").getPath().trim().replace('/', '\\');
            while (s.startsWith("\\")) {
                s = s.substring(1);
            }

            while (s.endsWith("\\")) {
                s = s.substring(0, s.length() - 1);
            }

            subDirPath = subDirPath.replaceAll("/", "\\");
            while (subDirPath.startsWith("\\")) {
                subDirPath = subDirPath.substring(1);
            }

            return s + "\\" + subDirPath;
        } else {
            String s = this.getClass().getResource("/").getPath().trim();
            while (s.endsWith("/")) {
                s = s.substring(0, s.length() - 1);
            }

            subDirPath = subDirPath.replaceAll("\\\\", "/");
            while (subDirPath.startsWith("\\")) {
                subDirPath = subDirPath.substring(1);
            }

            return s + "/" + subDirPath;
        }
    }
}