package com.suy.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



class Replace {
    int start;
    int end;

    public Replace() {
    }

    public Replace(int start, int end) {
        this.start = start;
        this.end = end;
    }
}

class ParamsReplace extends Replace {
    String param;

    public ParamsReplace() {
    }

    public ParamsReplace(int start, int end, String param) {
        super(start, end);
        this.param = param;
    }
}

class StringReplace extends Replace {
    String str;

    public StringReplace() {
    }

    public StringReplace(int start, int end, String str) {
        super(start, end);
        this.str = str;
    }
}

class EscapeReplace extends StringReplace {
    boolean isEscape;

    public EscapeReplace() {
    }

    public EscapeReplace(int start, int end, String str, boolean isEscape) {
        super(start, end, str);
        this.isEscape = isEscape;
    }
}

class MyNode {
    protected String name;                                          // 节点名称(根节点为.)
    protected Map<String, Object> vars = new LinkedHashMap<>();     // 变量映射表
    protected List<ParamsReplace> params = new ArrayList<>();       // 需要被替换的参数列表
    protected List<StringReplace> replaces = new ArrayList<>();     // 需要被替换的静态字符串列表
    transient protected MyNode parent;                              // 父节点
    protected String src;                                           // 源字符串

    static final Pattern pattern = Pattern.compile("\\$\\{|\\$\\(|}|\\)`|`");

    private static class MyUtil {
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
        private static final Logger LOGGER = LogManager.getLogger();
    }

    private static class MyNodeStatus extends MyNode {
        transient private int srcOffset;
        transient private boolean isParam;
        transient private boolean isKey;
        transient private boolean isValue;
        transient private int keywordStart, keywordEnd;
        transient private int paramStart, paramEnd;
        transient private int keyStart, keyEnd;
        transient private int valueStart, valueEnd;
        transient private int paramRealStart, paramRealEnd;
        transient private int keyRealStart, valueRealEnd;
        transient private String key;
        transient private MyNodeStatus value;
        transient private EscapeReplace escape;

        public MyNodeStatus() {
        }

        public MyNodeStatus(String name, String src) {
            this.name = name;
            this.src = src;
        }

        public MyNodeStatus(String key, MyNodeStatus parentStatus) {
            this.srcOffset = parentStatus.valueStart;
            this.name = key;
            this.src = parentStatus.src;
            this.parent = parentStatus;
            parentStatus.value = this;
        }
    }

    public MyNode() {
    }

    public MyNode(String src) {
        this.src = src;
        parseSrc();
    }

    private void parseSrc() {
        if (src == null) {
            return;
        }

        MyNodeStatus rootStatus = new MyNodeStatus(name, src);
        MyNodeStatus status = rootStatus;
        Matcher matcher = pattern.matcher(src);

        while (true) {
            if (matcher.find()) {
                String match = matcher.group();
                MyUtil.LOGGER.trace("Match keyword %s [start_index: %d, end_index: %d]".
                        formatted(match, status.keywordStart, status.keywordEnd));

                switch (match) {
                    case "${" -> {
                        status.keywordStart = matcher.start();
                        status.keywordEnd = matcher.end();

                        if (status.isKey) {
                            MyUtil.LOGGER.warn("Except get ')`' but get '${");
                            clearKey(status);
                        }

                        if (status.isParam) {
                            MyUtil.LOGGER.warn("Except get '}' but get '${");
                            clearParam(status);
                        }

                        if ((status.escape = checkEscape(status)) != null) {
                            MyUtil.LOGGER.trace("Match escape %s [start_index: %d, end_index: %d], replace %s".
                                    formatted(status.src.substring(status.escape.start + status.srcOffset,
                                            status.escape.end + status.srcOffset), status.keywordStart,
                                            status.keywordEnd, status.escape.str));
                            if (!status.escape.isEscape) {
                                MyUtil.LOGGER.debug("Escape not effect, start parse param");
                                paramStart(status);
                            } else {
                                MyUtil.LOGGER.debug("Escape effect, skip parse param");
                                addEscape(status);
                            }
                        } else {
                            MyUtil.LOGGER.info("Start parse param");
                            paramStart(status);
                        }
                    }

                    case "$(" -> {
                        status.keywordStart = matcher.start();
                        status.keywordEnd = matcher.end();

                        if (status.isKey) {
                            MyUtil.LOGGER.warn("Except get ')`' but get '$(");
                            clearKey(status);
                        }

                        if (status.isParam) {
                            MyUtil.LOGGER.warn("Except get '}' but get '$(");
                            clearParam(status);
                        }

                        if ((status.escape = checkEscape(status)) != null) {
                            MyUtil.LOGGER.trace("Match escape %s [start_index: %d, end_index: %d], replace %s".
                                    formatted(status.src.substring(status.escape.start + status.srcOffset,
                                            status.escape.end + status.srcOffset), status.keywordStart,
                                            status.keywordEnd, status.escape.str));
                            if (!status.escape.isEscape) {
                                MyUtil.LOGGER.debug("Escape not effect, start parse key");
                                keyStart(status);
                            } else {
                                MyUtil.LOGGER.debug("Escape effect, skip parse key");
                                addEscape(status);
                            }
                        } else {
                            MyUtil.LOGGER.info("Start parse key");
                            keyStart(status);
                        }

                    }

                    case "}" -> {
                        status.keywordStart = matcher.start();
                        status.keywordEnd = matcher.end();

                        if (status.isParam && !status.isKey) {
                            paramEnd(status);
                        } else if (status.isKey) {
                            MyUtil.LOGGER.warn("Except get ')`' but get '}");
                            clearKey(status);
                        }
                    }

                    case ")`" -> {
                        status.keywordStart = matcher.start();
                        status.keywordEnd = matcher.end();

                        if (status.isKey && !status.isParam) {
                            String key = keyEnd(status);
                            if (key != null) {
                                MyUtil.LOGGER.info("Start parse value");
                                status = valueStart(key, status);
                            }
                        } else if (status.isParam) {
                            MyUtil.LOGGER.warn("Except get '}' but get ')`");
                            clearParam(status);
                        }
                    }

                    case "`" -> {
                        status.keywordStart = matcher.start();
                        status.keywordEnd = matcher.end();

                        if (status.isKey) {
                            MyUtil.LOGGER.warn("Except get ')`' but get '`");
                            clearKey(status);
                        }

                        if (status.isParam) {
                            MyUtil.LOGGER.warn("Except get '}' but get '`");
                            clearParam(status);
                        }

                        if (status != rootStatus) {
                            if ((status.escape = checkEscape(status)) != null) {
                                MyUtil.LOGGER.trace("Match escape %s [start_index: %d, end_index: %d], replace %s".
                                        formatted(status.src.substring(status.escape.start + status.srcOffset,
                                                status.escape.end + status.srcOffset), status.keywordStart,
                                                status.keywordEnd, status.escape.str));
                                if (!status.escape.isEscape) {
                                    MyUtil.LOGGER.debug("Escape not effect, stop parse value");
                                    addEscape(status);
                                    status = valueEnd(status);
                                } else {
                                    MyUtil.LOGGER.debug("Escape effect, continue parse value");
                                    addEscape(status);
                                }
                            } else {
                                MyUtil.LOGGER.info("Value parse completed");
                                status = valueEnd(status);
                            }
                        }
                    }
                }
            } else {
                break;
            }
        }

        if (status.isParam) {
            MyUtil.LOGGER.warn("Param missing keyword '}'");
        }

        if (status.isKey) {
            MyUtil.LOGGER.warn("Key missing keyword ')`'");
        }

        while (status != rootStatus) {
            MyUtil.LOGGER.warn("Value missing keyword '`'");
            status = valueEndNoClose(status);
        }

        this.params.addAll(rootStatus.params);
        this.replaces.addAll(rootStatus.replaces);
        this.vars.putAll(rootStatus.vars);

    }

    private static void paramStart(MyNodeStatus status) {
        status.isParam = true;
        status.paramStart = status.keywordEnd;
        status.paramRealStart = status.keywordStart;
    }

    private static void keyStart(MyNodeStatus status) {
        status.isKey = true;
        status.keyStart = status.keywordEnd;
        status.keyRealStart = status.keywordStart;
    }

    private static MyNodeStatus valueStart(String key, MyNodeStatus status) {
        status.isValue = true;
        status.valueStart = status.keywordEnd;
        return new MyNodeStatus(key, status);
    }

    private static void paramEnd(MyNodeStatus status) {
        status.paramEnd = status.keywordStart;
        status.paramRealEnd = status.keywordEnd;
        if (status.paramStart > status.paramEnd) {
            MyUtil.LOGGER.fatal("Logical error: paramStart > paramEnd");
            return;
        }
        String param = checkParam(status.src.substring(status.paramStart, status.paramEnd));
        if (param != null) {
            MyUtil.LOGGER.info("Param parse completed: [%s]".formatted(param));
            addParam(status, param);
            addEscape(status);
            status.isParam = false;
        } else {
            MyUtil.LOGGER.warn("Illegal param [%s]".formatted(status.src.substring(status.paramStart, status.paramEnd)));
            clearParam(status);
        }
    }

    private static String keyEnd(MyNodeStatus status) {
        status.keyEnd = status.keywordStart;
        if (status.keyStart > status.keyEnd) {
            MyUtil.LOGGER.fatal("Logical error: keyStart > keyEnd");
            return null;
        }
        String key = checkKey(status.src.substring(status.keyStart, status.keyEnd));
        if (key != null) {
            MyUtil.LOGGER.info("Key parse completed: [%s]".formatted(key));
            status.key = key;
            status.isKey = false;
        } else {
            MyUtil.LOGGER.warn("Illegal key [%s]".formatted(status.src.substring(status.keyStart, status.keyEnd)));
            clearKey(status);
        }
        return key;
    }

    private static MyNodeStatus valueEnd(MyNodeStatus status) {
        MyNodeStatus parentStatus = (MyNodeStatus) status.parent;
        parentStatus.valueEnd = status.keywordStart;
        parentStatus.valueRealEnd = status.keywordEnd;
        String value = parentStatus.src.substring(parentStatus.valueStart, parentStatus.valueEnd);

        if (status.params.size() == 0 && status.vars.size() == 0) {
            MyUtil.LOGGER.info("Value only string");
            parentStatus.vars.put(parentStatus.key, value);
        } else {
            MyUtil.LOGGER.info("Value have params or sub var");
            parentStatus.vars.put(parentStatus.key, status);
            status.src = value;
        }

        MyUtil.LOGGER.info("Var[%s] will be replaced with value[%s]".
                formatted(parentStatus.src.substring(parentStatus.keyRealStart, parentStatus.valueRealEnd).replaceAll("\\r\\n|\\r|\\n", "\\\\n"),
                        value.replaceAll("\\n", "\\\\n")));
        addParam(parentStatus, parentStatus.key);
        addEscape(parentStatus);

        parentStatus.key = null;
        parentStatus.value = null;
        parentStatus.isValue = false;

        return parentStatus;
    }

    private static MyNodeStatus valueEndNoClose(MyNodeStatus status) {
        MyNodeStatus parentStatus = (MyNodeStatus) status.parent;
        if (status.params != null) {
            status.params.forEach(param -> {
                param.start += status.srcOffset - parentStatus.srcOffset;
                param.end += status.srcOffset - parentStatus.srcOffset;
            });
            status.replaces.forEach(replace -> {
                replace.start += status.srcOffset - parentStatus.srcOffset;
                replace.end += status.srcOffset - parentStatus.srcOffset;
            });
            parentStatus.params.addAll(status.params);
            parentStatus.vars.putAll(status.vars);
            parentStatus.replaces.addAll(status.replaces);
        }
        return parentStatus;
    }

    private static String checkKey(String key) {
        key = key.trim();
        Pattern pattern = Pattern.compile("[\\s$\\\\/]");
        Matcher matcher = pattern.matcher(key);
        if (matcher.find()) {
            return null;
        }
        return key;
    }

    private static String checkParam(String param) {
        param = param.trim();
        Pattern pattern = Pattern.compile("[\\s$\\\\/]");
        Matcher matcher = pattern.matcher(param);
        if (matcher.find()) {
            return null;
        }
        return param;
    }

    private static void addParam(MyNodeStatus status, String param) {
        int start, end;
        if (status.isParam) {
            start = status.paramRealStart - status.srcOffset;
            end = status.paramRealEnd - status.srcOffset;
        } else if (status.isValue) {
            start = status.keyRealStart - status.srcOffset;
            end = status.valueRealEnd - status.srcOffset;
        } else {
            return;
        }
        status.params.add(new ParamsReplace(start, end, param));
    }

    private static void clearParam(MyNodeStatus status) {
        status.isParam = false;
        if (status.escape != null) {
            status.escape = null;
        }
    }

    private static void clearKey(MyNodeStatus status) {
        status.isKey = false;
        if (status.escape != null) {
            status.escape = null;
        }
    }

    private static EscapeReplace checkEscape(MyNodeStatus status) {
        EscapeReplace escape = null;
        int slashCount, escapeI = status.keywordStart - 1;
        String src = status.src;

        while (escapeI >= 0 && src.charAt(escapeI) == '\\') --escapeI;
        slashCount = status.keywordStart - 1 - escapeI;
        if (slashCount != 0) {
            escape = new EscapeReplace(escapeI + 1 - status.srcOffset, status.keywordStart - status.srcOffset,
                    "\\".repeat(slashCount / 2), slashCount % 2 != 0);
        }

        return escape;
    }

    private static void addEscape(MyNodeStatus status) {
        if (status.escape != null) {
            status.replaces.add(status.escape);
            status.escape = null;
        }
    }

    @Override
    public String toString() {
        return MyUtil.GSON.toJson(this);
    }


}

public class TextUtil {
    private MyNode node;

    public TextUtil(String src) {
        node = new MyNode(src);
        System.out.println(node);
    }
}

