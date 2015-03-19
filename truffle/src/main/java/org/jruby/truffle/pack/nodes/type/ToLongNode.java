package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.pack.nodes.SourceNode;
import org.jruby.truffle.runtime.RubyContext;

@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class ToLongNode extends PackNode {

    private final RubyContext context;

    @Child private CallDispatchHeadNode toIntNode;
    @CompilerDirectives.CompilationFinal private boolean seenInt;
    @CompilerDirectives.CompilationFinal private boolean seenLong;

    public ToLongNode(RubyContext context) {
        this.context = context;
    }

    public ToLongNode(ToLongNode prev) {
        context = prev.context;
        toIntNode = prev.toIntNode;
        seenInt = prev.seenInt;
        seenLong = prev.seenLong;
    }

    public abstract long executeToLong(VirtualFrame frame, Object object);

    @Specialization
    public long toLong(VirtualFrame frame, int object) {
        return object;
    }

    @Specialization
    public long toLong(VirtualFrame frame, long object) {
        return object;
    }

    @Specialization
    public long toLong(VirtualFrame frame, Object object) {
        if (toIntNode == null) {
            CompilerDirectives.transferToInterpreter();
            toIntNode = insert(DispatchHeadNodeFactory.createMethodCall(context, true));
        }

        final Object value = toIntNode.call(frame, object, "to_int", null);

        if (seenInt && value instanceof Integer) {
            return (int) value;
        }

        if (seenLong && value instanceof Long) {
            return (long) value;
        }

        CompilerDirectives.transferToInterpreterAndInvalidate();

        if (value instanceof Integer) {
            seenInt = true;
            return (int) value;
        }

        if (value instanceof Long) {
            seenLong = true;
            return (long) value;
        }

        throw new UnsupportedOperationException();
    }

}