package org.congocc.templates;

import org.congocc.templates.core.Environment;
import org.congocc.templates.core.EvaluationException;
import org.congocc.templates.core.nodes.DotExpression;
import org.congocc.templates.core.nodes.Macro;
import org.congocc.templates.core.parser.CTLLexer;
import org.congocc.templates.core.parser.CTLParser;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * An Extension is essentially what was
 * previously called a "builtin" in legacy FreeMarker
 * In Congo Templates, you can "register" your own
 * extensions.
 */
@FunctionalInterface
public interface Extension {
    Object get(DotExpression caller, Environment env);

    /**
     * @return the Extension of the given name
     */
    static Extension find(String name) {
        return Impl.knownExtensions.get(name);
    }

    /**
     * Register your own extension.
     * Note that the method that takes a
     * Function as a parameter may be easier
     * to use in most cases.
     */
    static void register(String name, Extension ext) {
        char firstChar = name.charAt(0);
        if (firstChar <= 0x7F && Character.isLetter(firstChar) && Character.isLowerCase(firstChar)) {
            throw new TemplateException("Illegal extension name: " + name + ". Extension names must start with an upper case letter.");
        }
        Impl.knownExtensions.put(name, ext);
    }

    /**
     * Register an extension function. If the name of the
     * extension is "foobar", then lhs.foobar will return
     * func.apply(lhs)
     */
    @SuppressWarnings("unchecked")
    static <T> void register(String name, Function<T,?> func) {
        register(name, (exp, env) -> {
            try {
               return func.apply((T) exp.lhs().evaluate(env));
            } catch (ClassCastException cce) {
                throw new EvaluationException(cce);
            }
        });
    }

    /**
     * Remove (or unregister, if you will) a given
     * extension. So, if you think that some standard
     * extension is dangerous or undesirable, just remove it.
     *
     * @param name the name of the extension to be removed.
     */
    static void remove(String name) {
        Impl.knownExtensions.remove(name);
    }

    static boolean isExtension(String name) {
        return Impl.knownExtensions.get(name) != null;
    }

    static void alias(String altName, String existingName) {
        Extension ext = find(existingName);
        if (ext == null) {
            throw new IllegalArgumentException("No extension " + existingName + " found.");
        }
        Impl.knownExtensions.put(altName, ext);
    }

    // Using inner class because you can't put a static initializer
    // in an interface
    static class Impl {
        private static Map<String, Extension> knownExtensions = new ConcurrentHashMap<>();
        static {
            register("URLEncode", Impl::URLEncode);
            register("Scope", (caller,env) -> {
                Object obj = caller.lhs().evaluate(env);
                if (obj instanceof Macro m) {
                    return env.getMacroContext(m);
                }
                throw new EvaluationException("Expecting macro");
            });
            register("Namespace", (caller,env) -> {
                Object obj = caller.lhs().evaluate(env);
                if (obj instanceof Macro m) {
                    return env.getMacroNamespace(m);
                }
                throw new EvaluationException("Expecting macro");
            });
            register("Source", (caller, env) -> caller.lhs().getSource());
            register("Reverse", Impl::Reverse);
            register("First", Impl::First);
            register("Last", Impl::Last);
            register("Size", Impl::Size);
            register("Keys", Impl::Keys);
            register("Values", Impl::Values);
            register("Capitalize", Impl::Capitalize);
            register("Chomp", Impl::Chomp);
            register("WordList", Impl::WordList);
            register("JavaStringEncode", Impl::JavaStringEncode);
            register("JavaScriptStringEncode", Impl::JavaScriptStringEncode);
            register("HTML", Impl::HTMLEncode);
            register("XML", Impl::XMLEncode);
            register("XHTML", Impl::XHTMLEncode);
            register("RTF", Impl::RTFEncode);
            register("Eval", Impl::Eval);
            register("C", Impl::C);
            register("Byte", Impl::byteCast);
            register("Double", Impl::doubleCast);
            register("Float", Impl::floatCast);
            register("Int", Impl::intCast);
            register("Long", Impl::longCast);
            register("Short", Impl::shortCast);
            register("Instanceof", Impl::IsInstance);
            alias("InstanceOf", "Instanceof");
            alias("Websafe", "HTML");
            //Allow the following because they are all keywords in Java anyway.
            knownExtensions.put("int", knownExtensions.get("Int"));
            knownExtensions.put("short", knownExtensions.get("Short"));
            knownExtensions.put("byte", knownExtensions.get("Byte"));
            knownExtensions.put("float", knownExtensions.get("Float"));
            knownExtensions.put("double", knownExtensions.get("Double"));
            knownExtensions.put("long", knownExtensions.get("Long"));
            knownExtensions.put("instanceof", knownExtensions.get("Instanceof"));
        }

        private static List<Object> Reverse(Object arg) {
            if (arg instanceof List l) {
                List<Object> result = new ArrayList<Object>(l);
                Collections.reverse(result);
                return result;
            }
            if (arg.getClass().isArray()) {
                List<?> l = Arrays.asList(arg);
                List<Object> result = new ArrayList<>(l);
                Collections.reverse(result);
                return result;
            }
            throw new EvaluationException("Expecting list or array");
        }

        private static Object First(Object arg) {
            if (arg instanceof List l) {
                if (l.size() == 0)
                    return null;
                return l.get(0);
            }
            if (arg.getClass().isArray()) {
                if (Array.getLength(arg) == 0)
                    return null;
                return Array.get(arg, 0);
            }
            throw new EvaluationException("Expecting a list or array");
        }

        private static Object Last(Object arg) {
            if (arg instanceof List l) {
                if (l.size() == 0)
                    return null;
                return l.get(l.size() - 1);
            }
            if (arg.getClass().isArray()) {
                if (Array.getLength(arg) == 0)
                    return null;
                return Array.get(arg, 0);
            }
            throw new EvaluationException("Expecting a list or an array");
        }

        private static int Size(Object arg) {
            if (arg instanceof Collection c) {
                return c.size();
            }
            if (arg instanceof Map m) {
                return m.size();
            }
            if (arg.getClass().isArray()) {
                return Array.getLength(arg);
            }
            throw new EvaluationException("Expecting a collection or a map or an array");
        }

        private static List<Object> Keys(Map<?,?> m) {
            return new ArrayList<Object>(m.keySet());
        }

        private static List<Object> Values(Map<?,?> m) {
            return new ArrayList<Object>(m.values());
        }

        private static int intCast(Number n) {
            return n.intValue();
        }

        private static long longCast(Number n) {
            return n.longValue();
        }

        private static float floatCast(Number n) {
            return n.floatValue();
        }

        private static double doubleCast(Number n) {
            return n.doubleValue();
        }

        private static byte byteCast(Number n) {
            return n.byteValue();
        }

        private static short shortCast(Number n) {
            return n.shortValue();
        }

        private static String Capitalize(CharSequence arg) {
            return StringUtil.capitalize(arg.toString());
        }

        private static String Chomp(CharSequence arg) {
            return StringUtil.chomp(arg.toString());
        }

        private static List<String> WordList(Object arg) {
            if (arg instanceof CharSequence) {
                String s = arg.toString();
                StringTokenizer st = new StringTokenizer(s);
                List<String> result = new ArrayList<>();
                while (st.hasMoreTokens()) {
                    result.add(st.nextToken());
                }
                return result;
            }
            throw new EvaluationException("Expecting a string");
        }

        private static String HTMLEncode(Object arg) {
            if (arg instanceof CharSequence) {
                String s = arg.toString();
                return StringUtil.HTMLEnc(s);
            }
            throw new EvaluationException("Expecting a string");
        }

        private static String XHTMLEncode(CharSequence arg) {
            return StringUtil.XHTMLEnc(arg.toString());
        }

        private static String XMLEncode(CharSequence arg) {
            return StringUtil.XMLEnc(arg.toString());
        }

        private static String JavaStringEncode(Object arg) {
            return StringUtil.javaStringEncode(arg.toString());
            /*
            if (arg instanceof CharSequence) {
                String s = arg.toString();
                return StringUtil.javaStringEncode(s);
            }
            throw new EvaluationException("Expecting a string");*/
        }

        private static String JavaScriptStringEncode(Object arg) {
            if (arg instanceof CharSequence) {
                String s = arg.toString();
                return StringUtil.javaScriptStringEnc(s);
            }
            throw new EvaluationException("Expecting a string");
        }

        private static String RTFEncode(Object arg) {
            if (arg instanceof CharSequence) {
                String s = arg.toString();
                return StringUtil.RTFEnc(s);
            }
            throw new EvaluationException("Expecting a string");
        }

        private static String URLEncode(Object arg) {
            if (arg instanceof CharSequence) {
                String s = arg.toString();
                return URLEncoder.encode(s, UTF_8);
            }
            throw new EvaluationException("Expecting a string");
        }

        public static Object Eval(DotExpression caller, Environment env) {
            String input = "(" + caller.lhs().evaluate(env) + ")";
            CTLLexer tokenSource = new CTLLexer("input", input, CTLLexer.LexicalState.EXPRESSION, caller.getBeginLine(),
                    caller.getBeginColumn());
            CTLParser parser = new CTLParser(tokenSource);
            parser.setTemplate(caller.getTemplate());
            var exp = parser.Expression();
            return exp.evaluate(env);
        }

        public static Function<Object,Boolean> IsInstance(DotExpression caller, Environment env) {
            Object object = caller.lhs().evaluate(env);
            return arg -> {
                Class<?> clazz = null;
                if (arg instanceof Class) {
                    clazz = (Class<?>) arg;
                }
                else if (arg instanceof CharSequence cs) {
                    try {
                        clazz = Class.forName(cs.toString());
                    } catch (Exception e) {
                        throw new EvaluationException(e);
                    }
                }
                else {
                    throw new EvaluationException("Expecting a class or the name of a class");
                }
                return clazz.isInstance(object);
            };
        }

        private static Object C(DotExpression caller, Environment env) {
            Object arg = caller.lhs().evaluate(env);
            if (arg instanceof Number num) {
                if (num instanceof Integer) {
                    // We accelerate this fairly common case
                    return num.toString();
                } else {
                    return (env == null ? Environment.getNewCNumberFormat() : env.getCNumberFormat()).format(num);
                }
            }
            else {
                throw new EvaluationException("Expecting a number on the left side of ?c");
            }
        }
    }
}
