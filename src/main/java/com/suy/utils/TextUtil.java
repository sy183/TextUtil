package com.suy.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MyUtil {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Logger LOGGER = LogManager.getLogger();
}

class Replace {
    int start;
    int end;
}

class ParamsReplace extends Replace {
    String param;
}

class StringReplace extends Replace {
    String str;
}

class MyNode {
    protected String name;                          // 节点名称(根节点为.)
    protected Map<String, Object> vars;             // 基本类型映射表(源字符串中的自定义变量不包含参数和自定义子变量的变量会被放入此Map)
    protected Map<String, ParamsReplace> params;    // 需要被替换的参数映射表
    protected Map<String, StringReplace> replaces;  // 需要被替换的静态字符串映射表
    protected Map<String, MyNode> children;         // 节点映射表(源字符串中的自定义变量包含参数或自定义子变量的变量会被放入此Map)
    protected MyNode parent;                        // 父节点
    protected String src;                           // 源字符串

    private static class MyNodeStatus extends MyNode {
        boolean isParam;
        boolean isKey;
        boolean isValue;
        StringReplace escape;
    }

    /**
     * 1. 遇到'${'
     *     1.1 如果标记param解析开始，继续查找
     *     1.2 如果遇到'}'，标记param结束
     */
    private void parseSrc() {

        if (src == null) {
            return;
        }

        MyNodeStatus rootStatus = new MyNodeStatus();
        MyNodeStatus status = rootStatus;

        /* '${' '$(' */
//        Pattern pattern1 = Pattern.compile("\\$\\{|\\$\\(");
        /* '${' '$(' '){' '}' */
        Pattern pattern = Pattern.compile("\\$\\{|\\$\\(|\\)\\{|}");
        Matcher matcher = pattern.matcher(src);

        while (true) {
            if (matcher.find()) {
                String match = matcher.group();
                if (match.equals("${")) {
                    if (!status.isKey && !status.isParam) {
                        // 开始解析param
                        // 判断前面是否有转义字符
                        if (true) {
                            // 添加转义字符到status中
                            // 判断是否被转义
                            if (true) {
                                continue;
                            } else {
                                status.isParam = true;
                            }
                        }
                    } else {
                        // param格式错误，清除param，如果有转义，清除转义信息
                        status.isParam = false;
                        if (status.escape != null) {
                            status.escape = null;
                        }
                    }
                } else if (match.equals("$(")) {
                    if (!status.isKey && !status.isParam) {
                        // 开始解析key
                        // 判断前面是否有转义字符
                        if (true) {
                            // 添加转义字符到status中
                            // 判断是否被转义
                            if (true) {
                                continue;
                            } else {
                                status.isParam = true;
                            }
                        }
                    } else {
                        // key格式错误，清除key，如果有转义，清除转义信息
                        status.isKey = false;
                        if (status.escape != null) {
                            status.escape = null;
                        }
                    }
                } else if (match.equals("){")) {
                    if (status.isKey && !status.isParam) {
                        // 检查key的合法性
                        if (true) {
                            // 开始解析value
                            // 创建对应的children
                            // 将status指针指向children
                            status.isValue = true;
                        } else {
                            // key不合法，清除key，如果有转义，清除转义信息
                            status.isKey = false;
                            if (status.escape != null) {
                                status.escape = null;
                            }
                        }
                    } else if (status.isParam) {
                        // param格式错误，清除param
                        status.isParam = false;
                        if (status.escape != null) {
                            status.escape = null;
                        }
                    }
                } else if (match.equals("}")) {
                    if (status.isParam && !status.isKey) {
                        // 检查param的合法性
                        if (true) {
                            // 将param添加到Map中
                        } else {
                            // param不合法，清除param，如果有转义，清除转义信息
                            status.isParam = false;
                            if (status.escape != null) {
                                status.escape = null;
                            }
                        }
                    } else if (status.isKey) {
                        // key格式错误，清除key，如果有转义，清除转义信息
                        status.isKey = false;
                        if (status.escape != null) {
                            status.escape = null;
                        }
                    }
                    if (!status.isParam && status != rootStatus) {
                        // 判断此节点是否只是单纯的字符串，若是则将此字符串添加到父节点的vars中
                        // status指针指回父节点
                        status = (MyNodeStatus) status.parent;
                    }
                }
            } else {
                break;
            }
        }

        while (status != rootStatus) {
            // 将此status的所有Map信息合并到父节点中
            // 将status指向其父节点
            status = (MyNodeStatus) status.parent;
        }

        // 将rootStatus添加到MyNode节点中

    }

    public static void main(String[] args) {
        MyNode myNode = new MyNode();
        myNode.src = "${dsd}, ${dsd}, $(a){30}";
        myNode.parseSrc();
    }
}

public class TextUtil {

}

