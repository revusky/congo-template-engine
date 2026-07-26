package org.congocc.templates.core.nodes;

import static org.congocc.templates.core.Wrap.JAVA_NULL;
import static org.congocc.templates.core.Wrap.LOOSE_NULL;
import static org.congocc.templates.core.Wrap.unwrap;
import static org.congocc.templates.core.Wrap.wrap;
import org.congocc.templates.core.reflection.*;
import org.congocc.templates.core.EvaluationException;
import org.congocc.templates.TemplateException;
import org.congocc.templates.core.Environment;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.List;
import java.util.ArrayList;

public class MethodCall extends TemplateNode implements Expression {

    public ArgsList getArgs() {
        return firstChildOfType(ArgsList.class);
    }

    public Expression lhs() {
        return (Expression) get(0);
    }

    public boolean isConstant() {
        return false;
    }

    public int numArgs() {
        ArgsList argsList = getArgs();
        return argsList == null ? 0 : argsList.numArgs();
    }

    @SuppressWarnings("unchecked")
    public Object evaluate(Environment env) {
        Object leftSide = lhs().evaluate(env);
        if (leftSide == LOOSE_NULL) return JAVA_NULL;
        ArgsList args = getArgs();
        if (leftSide instanceof Macro func) {
            env.setLastReturnValue(null);
            Appendable prevBuffer = env.getBuffer();
            StringBuilder newBuffer = new StringBuilder();
            try {
                env.setBuffer(newBuffer);
                env.render(func, args, null, null);
            } finally {
                env.setBuffer(prevBuffer);
            }
            return func.isFunction() ? env.getLastReturnValue() : newBuffer.toString();
        }
        if (args instanceof NamedArgsList) {
            throw new TemplateException("Named arguments not supported for Java method calls");
        }
        if (leftSide instanceof Supplier<?> supplier) {
            if (args != null && args.firstChildOfType(Expression.class) != null) {
                throw new EvaluationException("The method " + lhs() + " takes no arguments.");
            }
            return wrap(supplier.get());
        }
        List<Object> argumentList = args != null ? args.getParameterSequence(leftSide, env) : new ArrayList<>();
        if (leftSide instanceof Function func) {
            if (args == null || args.childrenOfType(Expression.class).size() != 1) {
                throw new EvaluationException("The method " + lhs() + " takes exactly one argument.");
            }
            Object arg = unwrap(argumentList.get(0));
            try {
                return wrap(func.apply(arg));
            } catch (ClassCastException cce) {
                throw new EvaluationException(cce);
            }
        }
        if (leftSide instanceof BiFunction bif) {
            if (argumentList.size() != 2) {
                throw new EvaluationException("The method " + lhs() + " takes exactly two arguments.");
            }
            try {
                return wrap(bif.apply(unwrap(argumentList.get(0)), unwrap(argumentList.get(1))));
            } catch (ClassCastException cce) {
                throw new EvaluationException(cce);
            }
        }
        if (leftSide instanceof TriFunction trif) {
            if (argumentList.size() != 3) {
                throw new EvaluationException("The method " + lhs() + " takes exactly three arguments.");
            }
            try {
               return wrap(trif.apply(unwrap(argumentList.get(0)), unwrap(argumentList.get(1)), unwrap(argumentList.get(2))));
            } catch (ClassCastException cce) {
                throw new EvaluationException(cce);
            }
        }
        if (leftSide instanceof QuadFunction quadf) {
            if (argumentList.size() != 4) {
                throw new EvaluationException("The method " + lhs() + " takes exactly four arguments.");
            }
            try {
                return wrap(quadf.apply(argumentList.get(0), argumentList.get(1), argumentList.get(2), argumentList.get(3)));
            } catch (ClassCastException cce) {
                throw new EvaluationException(cce);
            }
        }
        if (leftSide instanceof VarArgsFunction<?> targetMethod) {
            Object[] argArray =  argumentList.toArray();
            for (int i = 0; i < argArray.length; i++) {
                argArray[i] = unwrap(argArray[i]);
            }
            try {
               return wrap(targetMethod.apply(argArray));
            } catch (ClassCastException cce) {
                throw new EvaluationException(cce);
            }
        }
        if (numArgs() ==0 && lhs() instanceof DotExpression de) {
            if (de.isExtension()) {
                return leftSide;
            }
        }
        throw invalidTypeException(leftSide, lhs(), "method");
    }
}


