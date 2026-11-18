package com.oupao.wiz;

/**
 * 音乐页那几个以换行分隔的列表（自定义包名 / 隐藏列表）的行级工具。
 *
 * 为什么不能用 contains()：
 *   contains 是**子串匹配**。比如 custom 里已经有 "com.tencent.mm"，
 *   再选 "com.tencent.mm.ui" 时 contains 命中 → 判定成「已存在」→ 不写入 → 添加无效；
 *   反过来 isHid 用 contains 判断隐藏，也会被子串误伤。
 *   所以一律改成**逐行精确比较**。
 */
public class MusTxt {

    /** 列表里是否精确包含这一行 */
    public static boolean has(String list, String item) {
        if ((list == null) || (item == null) || (item.length() == 0)) {
            return false;
        }
        String[] lines = list.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().equals(item)) {
                return true;
            }
        }
        return false;
    }

    /** 追加一行（已存在则不重复加） */
    public static String add(String list, String item) {
        String cur = (list == null) ? "" : list;
        if (has(cur, item)) {
            return cur;
        }
        StringBuilder sb = new StringBuilder(cur);
        if ((sb.length() > 0) && (sb.charAt(sb.length() - 1) != '\n')) {
            sb.append("\n");
        }
        sb.append(item).append("\n");
        return sb.toString();
    }

    /** 去掉一行 */
    public static String remove(String list, String item) {
        if ((list == null) || (list.length() == 0)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String[] lines = list.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String t = lines[i].trim();
            if ((t.length() > 0) && (!t.equals(item))) {
                sb.append(t).append("\n");
            }
        }
        return sb.toString();
    }
}
