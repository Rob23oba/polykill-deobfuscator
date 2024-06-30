import org.mozilla.javascript.*;
import org.mozilla.javascript.ast.*;

import java.util.*;
import java.util.function.*;

import java.nio.charset.*;

public class ReadCode {
	public static void main(String[] args) throws Exception {
		main2(args); // align table
		String value = (String) java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);
		value = value.replace("let ", "var "); // rhino quirk

		Parser parser = new Parser();
		AstRoot root = parser.parse(value, "https://pastebin.com/raw/hAgtHd70", 1);

		root.visit(new ResolveConstantsNodeVisitor());
		//root.visit(new ReplaceElementGetByPropertyGet());
		root.visit(new CollectConstants());
		root.visit(new ApplyConstants());
		root.visit(new RemoveConstantNames(true));
		root.visit(new InvertDoubleNegation());
		root.visit(new RemoveParenthesesOnConstants());
		root.visit(new CollectFunctions());
		verifyFunctions();
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());

		usedFunctions.clear();
		root.visit(new CollectUsedFunctions());
		System.err.println(usedFunctions);
		usedFunctions.remove("a0_0xa0b8");
		root.visit(new RemoveUnusedFunctions());

		usedFunctions.clear();
		root.visit(new CollectUsedFunctions());
		root.visit(new RemoveUnusedFunctions());

		usedFunctions.clear();
		root.visit(new CollectUsedFunctions());
		root.visit(new RemoveUnusedFunctions());

		usedFunctions.clear();
		root.visit(new CollectUsedFunctions());
		root.visit(new RemoveUnusedFunctions());

		usedFunctions.clear();
		root.visit(new CollectUsedFunctions());
		root.visit(new RemoveUnusedFunctions());

		usedVarNames.clear();
		root.visit(new CollectVariables());
		root.visit(new RemoveUnusedVariables());

		//root.visit(new ReplaceElementGetByPropertyGet());

		usedNames.clear();
		objectConstants.clear();
		root.visit(new CollectConstants());
		root.visit(new ApplyConstants());

		objectFunctions.clear();
		root.visit(new CollectObjectFunctions());
		verifyObjectFunctions();
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());

		objectFunctions.clear();
		root.visit(new CollectObjectFunctions());
		verifyObjectFunctions();
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());

		objectFunctions.clear();
		root.visit(new CollectObjectFunctions());
		verifyObjectFunctions();
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());

		objectFunctions.clear();
		root.visit(new CollectObjectFunctions());
		verifyObjectFunctions();
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());
		root.visit(new ResolveConstantsNodeVisitor());

		usedNames.clear();
		objectConstants.clear();
		root.visit(new CollectConstants());
		root.visit(new ApplyConstants());
		root.visit(new RemoveConstantNames(false));
		root.visit(new RemoveUnusedIfPaths());
		root.visit(new RemoveLonelyBlocks());
		root.visit(new RemoveLonelyBlocks());
		root.visit(new RemoveLonelyBlocks());
		root.visit(new SplitVariableDefinitions());
		root.visit(new RemoveIfElseNesting());

		usedVarNames.clear();
		root.visit(new CollectVariables());
		root.visit(new RemoveUnusedVariables());

		StringBuilder res = new StringBuilder();
		stringifyNode(root, res, "", "    ");
		System.out.println(res);
	}

	// value null implies value being overridden somewhere
	static Map<String, Object> constants = new HashMap<>(); // within variables
	static Map<String, Object> objectConstants = new HashMap<>(); // within objects
	static Map<String, FunctionNode> functions = new HashMap<>(); // functions within blocks
	static Map<String, FunctionNode> objectFunctions = new HashMap<>(); // functions within objects

	static Set<String> usedNames = new HashSet<>();
	static Set<String> usedFunctions = new HashSet<>();
	static Set<String> usedVarNames = new HashSet<>();

	public static void assignValue(String varName, Object constant) {
		if (constant instanceof AstNode) {
			constant = null;
		}
		if (constants.containsKey(varName)) {
			constants.put(varName, null);
		} else {
			constants.put(varName, constant);
		}
	}

	public static void assignObjectValue(String varName, Object constant) {
		if (constant instanceof AstNode) {
			constant = null;
		}
		if (objectConstants.containsKey(varName)) {
			objectConstants.put(varName, null);
		} else {
			objectConstants.put(varName, constant);
		}
	}

	public static class InvertDoubleNegation implements NodeVisitor {
		public boolean visit(AstNode node) {
			if (node instanceof InfixExpression e) {
				int type = e.getType();
				if (type != Token.ADD && type != Token.SUB) {
					return true;
				}
				AstNode r = e.getRight();
				if (r instanceof NumberLiteral n && n.getNumber() < 0) {
					n.setNumber(-n.getNumber());
					n.setValue(numberString(n.getNumber()));
					e.setType(type == Token.ADD ? Token.SUB : Token.ADD);
				} else if (r instanceof UnaryExpression u && u.getType() == Token.NEG) {
					e.setRight(u.getOperand());
					e.setType(type == Token.ADD ? Token.SUB : Token.ADD);
				}
			}
			return true;
		}
	}

	public static class RemoveParenthesesOnConstants implements NodeVisitor {
		public boolean visit(AstNode node) {
			if (node instanceof ParenthesizedExpression o) {
				AstNode n = o.getExpression();
				if (n instanceof NumberLiteral || n instanceof StringLiteral) {
					replaceChildOfNode(node.getParent(), node, n);
					return false;
				}
			}
			return true;
		}
	}

	public static class ReplaceElementGetByPropertyGet implements NodeVisitor {
		public boolean visit(AstNode node) {
			if (node instanceof ElementGet x) {
				AstNode el = x.getElement();
				Object value = resolveConstantExpression(el);
				if (value == null || value instanceof AstNode || !(value instanceof String nm)) {
					return true;
				}
				PropertyGet p = new PropertyGet(x.getPosition(), x.getLength(), x.getTarget(), new Name(el.getPosition(), el.getLength(), nm));
				p.setOperatorPosition(x.getLb());
				replaceChildOfNode(node.getParent(), node, p);
			}
			return true;
		}
	}

	public static class CollectConstants implements NodeVisitor {
		public boolean visit(AstNode node) {
			if (node instanceof ObjectLiteral o) {
				for (ObjectProperty p : o.getElements()) {
					String name;
					if (p.getLeft() instanceof Name nm) {
						name = nm.getIdentifier();
					} else {
						Object val = resolveConstantExpression(p.getLeft());
						if (val == null || val instanceof AstNode) continue;
						if (!(val instanceof String)) throw new Error("" + val);
						name = val.toString();
					}
					assignObjectValue(name, resolveConstantExpression(p.getRight()));
				}
			}
			return true;
		}
	}

	public static class ApplyConstants implements NodeVisitor {
		public boolean visit(AstNode node) {
			if (node instanceof PropertyGet x) {
				String name = x.getProperty().getIdentifier();

				Object value = objectConstants.get(name);
				if (value != null) {
					replaceChildOfNode(node.getParent(), node, constantNodeReplacing(node, value));
					return false;
				} else {
					usedNames.add(name);
				}
			}
			return true;
		}
	}

	public static class RemoveConstantNames implements NodeVisitor {
		boolean check0X;

		public RemoveConstantNames(boolean check0X) {
			this.check0X = check0X;
		}

		public boolean visit(AstNode node) {
			if (node instanceof ObjectLiteral o) {
				ListIterator<ObjectProperty> it = o.getElements().listIterator();
				while (it.hasNext()) {
					ObjectProperty p = it.next();
					String name;
					if (p.getLeft() instanceof Name nm) {
						name = nm.getIdentifier();
					} else {
						Object val = resolveConstantExpression(p.getLeft());
						if (val == null || val instanceof AstNode) continue;
						if (!(val instanceof String)) throw new Error("" + val);
						name = val.toString();
					}
					if (!usedNames.contains(name) && (!check0X || name.startsWith("_0x"))) {
						it.remove();
					}
				}
			}
			return true;
		}
	}

	public static class ResolveConstantsNodeVisitor implements NodeVisitor {
		public boolean visit(AstNode node) {
			if (node instanceof NumberLiteral n) {
				return false;
			}
			if (node instanceof StringLiteral n) {
				return false;
			}
			Object constant = resolveConstantExpression(node);
			if (constant instanceof AstNode n) {
				if (node instanceof FunctionCall && n != node) {
					replaceChildOfNode(node.getParent(), node, n);
					return false;
				}
				return true;
			}
			replaceChildOfNode(node.getParent(), node, constantNodeReplacing(node, constant));
			return false;
		}
	}

	public static class CollectFunctions implements NodeVisitor {
		public boolean visit(AstNode node) {
			if (node instanceof FunctionNode n && !n.getName().isEmpty()) {
				functions.put(n.getName(), n);
			}
			return true;
		}
	}

	public static class InlineConstantFunctions implements NodeVisitor {
		public boolean visit(AstNode node) {
			if (node instanceof FunctionCall c) {
				if (!(c.getTarget() instanceof Name nm)) {
					return true;
				}
				String name = nm.getIdentifier();
				FunctionNode fn = functions.get(name);
				if (fn == null) {
					return true;
				}
			}
			return true;
		}
	}

	public static class CollectUsedFunctions implements NodeVisitor {
		public boolean visit(AstNode node) {
			if (node instanceof FunctionCall c) {
				if (!(c.getTarget() instanceof Name nm)) {
					return true;
				}
				String name = nm.getIdentifier();
				usedFunctions.add(name);
			}
			if (node instanceof Name nm && node.getParent() instanceof FunctionCall) {
				usedFunctions.add(nm.getIdentifier());
			}
			return true;
		}
	}

	public static class RemoveUnusedFunctions implements NodeVisitor {
		public boolean visit(AstNode node) {
			if (node instanceof FunctionNode n) {
				if (!n.getName().isEmpty() && !usedFunctions.contains(n.getName())) {
					Node paren = n.getParent();
					if (paren instanceof Block) {
						paren.removeChild(n);
					} else if (paren instanceof AstRoot) {
						paren.removeChild(n);
					} else {
						throw new IllegalArgumentException("Node " + paren.getClass());
					}
				}
			}
			return true;
		}
	}

	public static class CollectObjectFunctions implements NodeVisitor {
		public boolean visit(AstNode node) {
			if (node instanceof ObjectLiteral o) {
				for (ObjectProperty p : o.getElements()) {
					String name;
					if (p.getLeft() instanceof Name nm) {
						name = nm.getIdentifier();
					} else {
						Object val = resolveConstantExpression(p.getLeft());
						if (val == null || val instanceof AstNode) continue;
						if (!(val instanceof String)) throw new Error("" + val);
						name = val.toString();
					}
					if (p.getRight() instanceof FunctionNode fn) {
						objectFunctions.put(name, fn);
					}
				}
			}
			return true;
		}
	}

	public static class RemoveUnusedIfPaths implements NodeVisitor {
		public boolean visit(AstNode node) {
			if (node instanceof IfStatement stat) {
				Object val = resolveConstantExpression(stat.getCondition());
				if (!(val instanceof Boolean b)) {
					return true;
				}
				Node repl = b ? stat.getThenPart() : stat.getElsePart();
				Iterable<Node> statements = repl instanceof Block ? repl : Collections.singleton(repl);
				replaceStatement(node.getParent(), node, statements);
			}
			if (node instanceof ConditionalExpression expr) {
				Object val = resolveConstantExpression(expr.getTestExpression());
				if (!(val instanceof Boolean b)) {
					return true;
				}
				AstNode repl = b ? expr.getTrueExpression() : expr.getFalseExpression();
				replaceChildOfNode(node.getParent(), node, repl);
			}
			return true;
		}
	}

	public static class CollectVariables implements NodeVisitor {
		public boolean visit(AstNode node) {
			if (node instanceof Name n && !(n.getParent() instanceof VariableInitializer)) {
				usedVarNames.add(n.getIdentifier());
			}
			return true;
		}
	}

	public static class RemoveUnusedVariables implements NodeVisitor {
		public boolean visit(AstNode node) {
			if (node instanceof VariableDeclaration n) {
				ListIterator<VariableInitializer> vars = n.getVariables().listIterator();
				while (vars.hasNext()) {
					AstNode target = vars.next().getTarget();
					if (target instanceof Name nm && !usedVarNames.contains(nm.getIdentifier())) {
						vars.remove();
					}
				}
				if (n.getVariables().isEmpty()) {
					replaceStatement(node.getParent(), node, Collections.emptyList());
				}
			}
			return true;
		}
	}

	public static class RemoveLonelyBlocks implements NodeVisitor {
		public boolean visit(AstNode node) {
			if (node.getClass() == Block.class) {
				//System.err.println(node.getParent().getClass());
			}
			if (node.getClass() == Scope.class) {
				//System.err.println(node.getParent().getClass() + " " + node.getParent().toSource());
				if (node.getParent().getClass() == Scope.class || node.getParent() instanceof Block) {
					//System.err.println(node.getParent().toSource());
					replaceStatement(node.getParent(), node, node);
				}
			}
			return true;
		}
	}

	public static class SplitVariableDefinitions implements NodeVisitor {
		public boolean visit(AstNode node) {
			if (node instanceof VariableDeclaration n) {
				List<VariableInitializer> vars = new ArrayList<>(n.getVariables());
				while (vars.size() > 1) {
					n.getVariables().clear();
					n.addVariable(vars.remove(0));

					VariableDeclaration newDecl = new VariableDeclaration();
					newDecl.setType(n.getType());
					newDecl.setIsStatement(true);

					n.getParent().addChildAfter(newDecl, n);
					newDecl.setParent(n.getParent());

					n = newDecl;
				}
				n.getVariables().clear();
				n.addVariable(vars.remove(0));
			}
			return true;
		}
	}

	public static class RemoveIfElseNesting implements NodeVisitor {
		public boolean visit(AstNode node) {
			if (node instanceof IfStatement s) {
				AstNode elseStat = s.getElsePart();
				if (elseStat instanceof Scope sc && sc.getClass() == Scope.class) {
					Node first = sc.getFirstChild();
					if (first instanceof IfStatement s2 && first.getNext() == null) {
						s.setElsePart(s2);
					}
				}
			}
			return true;
		}
	}

	public static void verifyFunctions() {
		for (Map.Entry<String, FunctionNode> entry : functions.entrySet()) {
			if (entry.getValue() != null && !verifyFunction(entry.getKey(), entry.getValue())) {
				entry.setValue(null);
			}
		}
	}

	public static void verifyObjectFunctions() {
		for (Map.Entry<String, FunctionNode> entry : objectFunctions.entrySet()) {
			if (entry.getValue() != null && !verifyFunction(entry.getKey(), entry.getValue())) {
				//System.err.println("denied " + entry.getKey() + " = " + entry.getValue().toSource());
				entry.setValue(null);
			} else {
				//System.err.println("accepted " + entry.getKey() + " = " + entry.getValue().toSource());
			}
		}
	}

	public static boolean verifyFunction(String name, FunctionNode n) {
		Block block = (Block) n.getBody();
		int countx = 0;
		for (Node stat : block) {
			if (!(stat instanceof ReturnStatement)) {
				// must be a function _0xabcdef(_0x012345, _0x987654, ...) { return ...; }
				return false;
			}
			countx++;
		}
		if (countx != 1) {
			return false;
		}
		Map<String, Integer> nameUsage = new HashMap<>();
		block.visit(node -> {
			if (node instanceof Name x) {
				nameUsage.merge(x.getIdentifier(), 1, (a, b) -> a + b);
			}
			return true;
		});
		Set<String> paramNames = new HashSet<>();
		for (AstNode pname : n.getParams()) {
			if (!(pname instanceof Name nm)) {
				throw new IllegalArgumentException("bad param " + pname.getClass() + " (" + pname.toSource() + ") in " + name);
			}
			paramNames.add(nm.getIdentifier());
		}
		for (Map.Entry<String, Integer> usages : nameUsage.entrySet()) {
			String vname = usages.getKey();
			if (functions.containsKey(vname)) {
				// Function references allowed
				continue;
			}
			if (!paramNames.contains(vname)) {
				// Reference to outer value -> closure
				return false;
			}
		}
		return true;
	}

	public static String numberString(double number) {
		String str = Double.toString(number);
		if (str.endsWith(".0")) {
			str = str.substring(0, str.length() - 2);
		}
		return str;
	}

	public static AstNode constantNodeReplacing(AstNode original, Object value) {
		if (original instanceof NumberLiteral || original instanceof StringLiteral) {
			return original;
		}
		if (value instanceof Number n) {
			double d = n.doubleValue();
			NumberLiteral l = new NumberLiteral(original.getPosition(), numberString(d), n.doubleValue());
			l.setLength(original.getLength());
			return l;
		} else if (value instanceof String s) {
			StringLiteral lit = new StringLiteral(original.getPosition(), original.getLength());
			lit.setValue(s);
			lit.setQuoteCharacter('\'');
			return lit;
		} else if (value instanceof Boolean b) {
			int token = b.booleanValue() ? Token.TRUE : Token.FALSE;
			return new KeywordLiteral(original.getPosition(), original.getLength(), token);
		}
		throw new IllegalArgumentException("Unexpected constant " + value + " by type " + value.getClass());
	}

	public static Object plus(Object left, Object right) {
		if (left instanceof Number n && right instanceof Number n2) {
			return n.doubleValue() + n2.doubleValue();
		}
		if (left instanceof String s && right instanceof String s2) {
			return s + s2;
		}
		return null;
	}

	public static Object minus(Object left, Object right) {
		if (left instanceof Number n && right instanceof Number n2) {
			return n.doubleValue() - n2.doubleValue();
		}
		if (left instanceof String || right instanceof String) {
			return Double.NaN;
		}
		return null;
	}

	public static Object multiply(Object left, Object right) {
		if (left instanceof Number n && right instanceof Number n2) {
			return n.doubleValue() * n2.doubleValue();
		}
		return null;
	}

	public static Object equality(Object left, Object right) {
		if (left instanceof String s && right instanceof String s2) {
			return s.equals(s2);
		}
		return null;
	}

	public static Object inequality(Object left, Object right) {
		if (left instanceof String s && right instanceof String s2) {
			return !s.equals(s2);
		}
		return null;
	}

	public static Object negate(Object left) {
		if (left instanceof Number n) {
			return -n.doubleValue();
		}
		return null;
	}

	public static Object not(Object left) {
		if (left instanceof Boolean b) {
			return !b.booleanValue();
		}
		return null;
	}

	public static void replaceStatement(Node parent, Node child, Iterable<? extends Node> statements) {
		List<Node> list = new ArrayList<>();
		for (Node n : statements) {
			list.add(n);
		}
		for (Node n : list) {
			if (n.getNext() != null) {
				((AstNode) n).getParent().removeChild(n);
			}
			parent.addChildBefore(n, child);
			((AstNode) n).setParent((AstNode) parent);
		}
		parent.removeChild(child);
	}

	public static void replaceChildOfNode(Node parent, Node child, AstNode replacement) {
		switch (parent) {
			case InfixExpression x -> {
				if (x.getLeft() == child) {
					x.setLeft(replacement);
				} else if (x.getRight() == child) {
					x.setRight(replacement);
				}
			}
			case UnaryExpression x -> {
				if (x.getOperand() == child) {
					x.setOperand(replacement);
				}
			}
			case FunctionCall x -> {
				if (x.getTarget() == child) {
					x.setTarget(replacement);
				}
				List<AstNode> args = x.getArguments();
				for (int i = 0; i < args.size(); i++) {
					if (args.get(i) == child) {
						args.set(i, replacement);
						replacement.setParent(x);
					}
				}
			}
			case ParenthesizedExpression x -> {
				if (x.getExpression() == child) {
					x.setExpression(replacement);
				}
			}
			case ElementGet x -> {
				if (x.getElement() == child) {
					x.setElement(replacement);
				}
				if (x.getTarget() == child) {
					x.setTarget(replacement);
				}
			}
			case VariableInitializer x -> {
				if (x.getTarget() == child) {
					x.setTarget(replacement);
				}
				if (x.getInitializer() == child) {
					x.setInitializer(replacement);
				}
			}
			case ConditionalExpression x -> {
				if (x.getTestExpression() == child) {
					x.setTestExpression(replacement);
				}
				if (x.getTrueExpression() == child) {
					x.setTrueExpression(replacement);
				}
				if (x.getFalseExpression() == child) {
					x.setFalseExpression(replacement);
				}
			}
			case ArrayLiteral x -> {
				List<AstNode> elements = x.getElements();
				for (int i = 0; i < elements.size(); i++) {
					if (elements.get(i) == child) {
						elements.set(i, replacement);
					}
				}
			}
			case ReturnStatement x -> {
				if (x.getReturnValue() == child) {
					x.setReturnValue(replacement);
				}
			}
			case WhileLoop x -> {
				if (x.getCondition() == child) {
					x.setCondition(replacement);
				}
			}
			case IfStatement x -> {
				if (x.getCondition() == child) {
					x.setCondition(replacement);
				}
			}
			case ExpressionStatement x -> {
				if (x.getExpression() == child) {
					x.setExpression(replacement);
				}
			}
			default -> {
				throw new IllegalArgumentException(parent.getClass() + " unhandled");
			}
		}
	}

	public static AstNode createRecursiveCopy(AstNode node, Map<String, AstNode> replacements) {
		return switch (node) {
			case PropertyGet x -> {
				PropertyGet copy = new PropertyGet(node.getPosition(), node.getLength());
				copy.setLeft(createRecursiveCopy(x.getLeft(), replacements));
				copy.setRight(createRecursiveCopy(x.getRight(), replacements));
				copy.setOperator(x.getOperator());
				copy.setOperatorPosition(x.getOperatorPosition());
				yield copy;
			}
			case ObjectProperty x -> {
				ObjectProperty copy = new ObjectProperty(node.getPosition(), node.getLength());
				copy.setLeft(createRecursiveCopy(x.getLeft(), replacements));
				copy.setRight(createRecursiveCopy(x.getRight(), replacements));
				copy.setOperator(x.getOperator());
				copy.setOperatorPosition(x.getOperatorPosition());
				yield copy;
			}
			case Assignment x -> {
				Assignment copy = new Assignment(node.getPosition(), node.getLength());
				copy.setLeft(createRecursiveCopy(x.getLeft(), replacements));
				copy.setRight(createRecursiveCopy(x.getRight(), replacements));
				copy.setOperator(x.getOperator());
				copy.setOperatorPosition(x.getOperatorPosition());
				yield copy;
			}
			case InfixExpression x -> {
				InfixExpression copy = new InfixExpression(node.getPosition(), node.getLength());
				copy.setLeft(createRecursiveCopy(x.getLeft(), replacements));
				copy.setRight(createRecursiveCopy(x.getRight(), replacements));
				copy.setOperator(x.getOperator());
				copy.setOperatorPosition(x.getOperatorPosition());
				yield copy;
			}
			case UnaryExpression x -> {
				UnaryExpression copy = new UnaryExpression(node.getPosition(), node.getLength());
				copy.setOperand(createRecursiveCopy(x.getOperand(), replacements));
				copy.setOperator(x.getOperator());
				yield copy;
			}
			case ParenthesizedExpression x -> {
				ParenthesizedExpression copy = new ParenthesizedExpression(node.getPosition(), node.getLength());
				copy.setExpression(createRecursiveCopy(x.getExpression(), replacements));
				yield copy;
			}
			case FunctionCall x -> {
				FunctionCall copy = new FunctionCall(node.getPosition(), node.getLength());
				copy.setTarget(createRecursiveCopy(x.getTarget(), replacements));
				copy.setLp(x.getLp());
				copy.setRp(x.getRp());
				for (AstNode arg : x.getArguments()) {
					copy.addArgument(createRecursiveCopy(arg, replacements));
				}
				yield copy;
			}
			case ElementGet x -> {
				ElementGet copy = new ElementGet(node.getPosition(), node.getLength());
				copy.setTarget(createRecursiveCopy(x.getTarget(), replacements));
				copy.setLb(x.getLb());
				copy.setRb(x.getRb());
				copy.setElement(createRecursiveCopy(x.getElement(), replacements));
				yield copy;
			}
			case Name x -> {
				String name = x.getIdentifier();
				if (replacements.containsKey(name)) {
					yield replacements.get(name);
				}
				yield new Name(node.getPosition(), node.getLength(), name);
			}
			case NumberLiteral x -> {
				NumberLiteral copy = new NumberLiteral(node.getPosition(), node.getLength());
				copy.setValue(x.getValue());
				copy.setNumber(x.getNumber());
				yield copy;
			}
			case StringLiteral x -> {
				StringLiteral copy = new StringLiteral(node.getPosition(), node.getLength());
				copy.setValue(x.getValue());
				copy.setQuoteCharacter(x.getQuoteCharacter());
				yield copy;
			}
			default -> {
				throw new IllegalArgumentException(node.getClass() + " unhandled");
			}
		};
	}

	public static void stringifyList(Iterable<? extends AstNode> nodes, StringBuilder target, String indent, String indentAdd) {
		String del = "";
		for (AstNode n : nodes) {
			target.append(del);
			del = ", ";
			stringifyNode(n, target, indent, indentAdd);
		}
	}

	public static void stringifyLongList(Iterable<? extends Node> nodes, StringBuilder target, String indent, String indentAdd) {
		boolean hadOne = false;
		String newIndent = indent + indentAdd;
		for (Node n : nodes) {
			hadOne = true;
			target.append("\n").append(newIndent);
			stringifyNode((AstNode) n, target, newIndent, indentAdd);
		}

		if (hadOne) {
			target.append("\n").append(indent);
		}
	}

	public static void stringifyBlock(AstNode node, StringBuilder target, String indent, String indentAdd) {
		if (node instanceof Scope || node instanceof Block) {
			target.append(" ");
			stringifyNode(node, target, indent, indentAdd);
		} else {
			target.append("\n").append(indent + indentAdd);
			stringifyNode(node, target, indent + indentAdd, indentAdd);
		}
	}

	public static void blockSep(AstNode prev, StringBuilder target, String indent) {
		if (!(prev instanceof Scope) && !(prev instanceof Block)) {
			target.append("\n").append(indent);
		} else {
			target.append(" ");
		}
	}

	public static void stringifyNode(AstNode node, StringBuilder target, String indent, String indentAdd) {
		switch (node) {
			case AstRoot x -> {
				for (Node n : x) {
					target.append(indent);
					stringifyNode((AstNode) n, target, indent, indentAdd);
					target.append("\n");
				}
			}
			case ExpressionStatement x -> {
				stringifyNode(x.getExpression(), target, indent, indentAdd);
				target.append(";");
			}
			case ParenthesizedExpression x -> {
				target.append("(");
				stringifyNode(x.getExpression(), target, indent, indentAdd);
				target.append(")");
			}
			case FunctionCall x -> {
				stringifyNode(x.getTarget(), target, indent, indentAdd);
				target.append("(");
				stringifyList(x.getArguments(), target, indent, indentAdd);
				target.append(")");
			}
			case FunctionNode x -> {
				if (x.getFunctionType() == FunctionNode.ARROW_FUNCTION) {
					throw new IllegalArgumentException("arrow functions unhandled");
				}
				if (x.isMethod()) {
					throw new IllegalArgumentException("methods unhandled");
				}

				target.append("function");

				String name = x.getName();
				if (!name.isEmpty()) {
					target.append(" ").append(name);
				}
				target.append("(");
				stringifyList(x.getParams(), target, indent, indentAdd);
				target.append(")");
				stringifyBlock(x.getBody(), target, indent, indentAdd);
			}
			case Block x -> {
				target.append("{");
				stringifyLongList(x, target, indent, indentAdd);
				target.append("}");
			}
			case VariableDeclaration x -> {
				target.append(switch (x.getType()) {
					case Token.VAR -> "var";
					case Token.CONST -> "const";
					case Token.LET -> "let";
					default -> throw new IllegalArgumentException("Invalid variable declaration type " + x.getType());
				});
				target.append(" ");
				stringifyList(x.getVariables(), target, indent, indentAdd);
				if (x.isStatement()) {
					target.append(";");
				}
			}
			case VariableInitializer x -> {
				stringifyNode(x.getTarget(), target, indent, indentAdd);

				AstNode init = x.getInitializer();
				if (init != null) {
					target.append(" = ");
					stringifyNode(init, target, indent, indentAdd);
				}
			}
			case ObjectLiteral x -> {
				target.append("{");
				stringifyLongList(x.getElements(), target, indent, indentAdd);
				target.append("}");
			}
			case ObjectProperty x -> {
				if (x.isGetterMethod()) {
					target.append("get ");
				} else if (x.isSetterMethod()) {
					target.append("set ");
				}
				stringifyNode(x.getLeft(), target, indent, indentAdd);
				if (!x.isMethod()) {
					target.append(": ");
				}
				stringifyNode(x.getRight(), target, indent, indentAdd);
			}
			case ReturnStatement x -> {
				target.append("return");

				AstNode value = x.getReturnValue();
				if (value != null) {
					target.append(" ");
					stringifyNode(value, target, indent, indentAdd);
				}
				target.append(";");
			}
			case PropertyGet x -> {
				stringifyNode(x.getLeft(), target, indent, indentAdd);
				target.append(".");
				stringifyNode(x.getRight(), target, indent, indentAdd);
			}
			case InfixExpression x -> {
				if (!(x instanceof Assignment) && x.getClass() != InfixExpression.class) {
					throw new IllegalArgumentException(node.getClass() + " unhandled");
				}
				stringifyNode(x.getLeft(), target, indent, indentAdd);
				if (x.getType() != Token.COMMA) {
					target.append(" ");
				}
				target.append(AstNode.operatorToString(x.getType()));
				target.append(" ");
				stringifyNode(x.getRight(), target, indent, indentAdd);
			}
			case UnaryExpression x -> {
				int type = x.getType();
				target.append(AstNode.operatorToString(type));
				if (type == Token.TYPEOF || type == Token.DELPROP || type == Token.VOID) {
					target.append(" ");
				}
				stringifyNode(x.getOperand(), target, indent, indentAdd);
			}
			case WhileLoop x -> {
				target.append("while (");
				stringifyNode(x.getCondition(), target, indent, indentAdd);
				target.append(")");
				stringifyBlock(x.getBody(), target, indent, indentAdd);
			}
			case ArrayLiteral x -> {
				target.append("[");
				stringifyList(x.getElements(), target, indent, indentAdd);
				target.append("]");
			}
			case ForLoop x -> {
				target.append("for (");
				stringifyNode(x.getInitializer(), target, indent, indentAdd);
				target.append("; ");
				stringifyNode(x.getCondition(), target, indent, indentAdd);
				target.append("; ");
				stringifyNode(x.getIncrement(), target, indent, indentAdd);
				target.append(")");
				stringifyBlock(x.getBody(), target, indent, indentAdd);
			}
			case Scope x -> {
				if (x.getClass() != Scope.class) {
					throw new IllegalArgumentException(node.getClass() + " unhandled");
				}
				target.append("{");
				stringifyLongList(x, target, indent, indentAdd);
				target.append("}");
			}
			case TryStatement x -> {
				target.append("try ");
				stringifyNode(x.getTryBlock(), target, indent, indentAdd);
				for (CatchClause cc : x.getCatchClauses()) {
					target.append(" ");
					stringifyNode(cc, target, indent, indentAdd);
				}
				AstNode finallyBlock = x.getFinallyBlock();
				if (finallyBlock != null) {
					target.append(" finally");
					stringifyBlock(x.getFinallyBlock(), target, indent, indentAdd);
				}
			}
			case CatchClause x -> {
				target.append("catch (");
				stringifyNode(x.getVarName(), target, indent, indentAdd);

				AstNode condition = x.getCatchCondition();
				if (condition != null) {
					target.append(" if ");
					stringifyNode(condition, target, indent, indentAdd);
				}
				target.append(") ");
				stringifyNode(x.getBody(), target, indent, indentAdd);
			}
			case IfStatement x -> {
				target.append("if (");
				stringifyNode(x.getCondition(), target, indent, indentAdd);
				target.append(")");
				stringifyBlock(x.getThenPart(), target, indent, indentAdd);
				AstNode elseStatement = x.getElsePart();
				if (elseStatement != null) {
					blockSep(x.getThenPart(), target, indent);
					target.append("else");
					if (elseStatement instanceof IfStatement) {
						// else if
						target.append(" ");
						stringifyNode(elseStatement, target, indent, indentAdd);
					} else {
						stringifyBlock(elseStatement, target, indent, indentAdd);
					}
				}
			}
			case BreakStatement x -> {
				target.append("break");

				Name label = x.getBreakLabel();
				if (label != null) {
					stringifyNode(label, target, indent, indentAdd);
				}
				target.append(";");
			}
			case ContinueStatement x -> {
				target.append("continue");

				Name label = x.getLabel();
				if (label != null) {
					stringifyNode(label, target, indent, indentAdd);
				}
				target.append(";");
			}
			case ElementGet x -> {
				stringifyNode(x.getTarget(), target, indent, indentAdd);
				target.append("[");
				stringifyNode(x.getElement(), target, indent, indentAdd);
				target.append("]");
			}
			case KeywordLiteral x -> {
				target.append(switch (x.getType()) {
					case Token.THIS -> "this";
					case Token.NULL -> "null";
					case Token.TRUE -> "true";
					case Token.FALSE -> "false";
					case Token.DEBUGGER -> "debugger;";
					default -> throw new IllegalArgumentException("Invalid keyword type " + x.getType());
				});
			}
			case ConditionalExpression x -> {
				stringifyNode(x.getTestExpression(), target, indent, indentAdd);
				target.append(" ? ");
				stringifyNode(x.getTrueExpression(), target, indent, indentAdd);
				target.append(" : ");
				stringifyNode(x.getFalseExpression(), target, indent, indentAdd);
			}
			case UpdateExpression x -> {
				String op = AstNode.operatorToString(x.getType());
				if (x.isPrefix()) {
					target.append(op);
				}
				stringifyNode(x.getOperand(), target, indent, indentAdd);
				if (x.isPostfix()) {
					target.append(op);
				}
			}
			case Name _, NumberLiteral _, StringLiteral _, BigIntLiteral _, RegExpLiteral _ -> {
				target.append(node.toSource(0));
			}
			default -> {
				throw new IllegalArgumentException(node.getClass() + " unhandled");
			}
		}
	}

	public static Object resolveConstantExpression(Node n) {
		if (n == null) {
			throw new NullPointerException();
		}
		if (n instanceof ParenthesizedExpression x) {
			return resolveConstantExpression(x.getExpression());
		}
		if (n instanceof StringLiteral x) {
			return x.getValue();
		}
		if (n instanceof NumberLiteral x) {
			return x.getNumber();
		}
		if (n instanceof InfixExpression x) {
			int type = x.getType();
			BinaryOperator<Object> op = switch (x.getType()) {
				case Token.ADD -> ReadCode::plus;
				case Token.SUB -> ReadCode::minus;
				case Token.MUL -> ReadCode::multiply;
				case Token.EQ, Token.SHEQ -> ReadCode::equality;
				case Token.NE, Token.SHNE -> ReadCode::inequality;
				default -> null;
			};
			if (op == null) {
				return n;
			}

			Object left = resolveConstantExpression(x.getLeft());
			if (left instanceof AstNode) {
				return n;
			}
			Object right = resolveConstantExpression(x.getRight());
			if (right instanceof AstNode) {
				return n;
			}
			Object result = op.apply(left, right);
			if (result != null) {
				return result;
			}
		}
		if (n instanceof UnaryExpression x) {
			int type = x.getType();
			if (type == Token.NOT) {
				if (x.getOperand() instanceof ArrayLiteral l && l.getElements().isEmpty()) {
					return Boolean.FALSE;
				}
			}
			UnaryOperator<Object> op = switch (x.getType()) {
				case Token.NEG -> ReadCode::negate;
				case Token.NOT -> ReadCode::not;
				default -> null;
			};
			if (op == null) {
				return n;
			}

			Object left = resolveConstantExpression(x.getOperand());
			if (left instanceof AstNode) {
				return n;
			}
			Object result = op.apply(left);
			if (result != null) {
				return result;
			}
		}
		if (n instanceof KeywordLiteral x) {
			if (x.getType() == Token.TRUE) {
				return Boolean.TRUE;
			}
			if (x.getType() == Token.FALSE) {
				return Boolean.FALSE;
			}
		}
		if (n instanceof FunctionCall c) {
			// inline constant functions
			String name;
			FunctionNode fn;
			if (c.getTarget() instanceof Name nm) {
				name = nm.getIdentifier();
				fn = functions.get(name);
			} else if (c.getTarget() instanceof PropertyGet pg) {
				name = pg.getProperty().getIdentifier();
				fn = objectFunctions.get(name);
			} else {
				//System.err.println("Neither, it is a " + c.getTarget().getClass() + " (it is " + c.getTarget().toSource() + ")");
				return n;
			}
			if ("a0_0xa0b8".equals(name)) {
				Object[] args = new Object[2];
				int i = 0;
				for (AstNode arg : c.getArguments()) {
					Object constant = resolveConstantExpression(arg);
					if (constant == null || constant instanceof AstNode) {
						return n;
					}
					args[i++] = constant;
				}
				String result = decodingFunction(((Number) args[0]).intValue(), (String) args[1]);
				return result;
			}
			if (fn == null) {
				return n;
			}
			Map<String, AstNode> replacements = new HashMap<>(c.getArguments().size());
			Iterator<AstNode> paramNames = fn.getParams().iterator();
			for (AstNode arg : c.getArguments()) {
				/*Object constant = resolveConstantExpression(arg);
				if (!(arg instanceof NumberLiteral) && !(arg instanceof StringLiteral) && !(arg instanceof Name)) {
					return n;
				}*/
				String pname = ((Name) paramNames.next()).getIdentifier();
				replacements.put(pname, arg);
			}
			ReturnStatement stat = null;
			for (Node x : fn.getBody()) {
				stat = (ReturnStatement) x;
			}
			return resolveConstantExpression(createRecursiveCopy(stat.getReturnValue(), replacements));
		}
		return n;
	}

	public static class Decoder {
		// _0x562627
		// base 64 decode obfuscated swapped case
		public static String base64Decode(String value) {
			String charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/=";
			String result = "";
			int prevChIndex = 0;
			int clock = 0;
			int index = 0;
			while (index < value.length()) {
				char ch = value.charAt(index++);
				int chIndex = charset.indexOf(ch);
				if (chIndex == -1) {
					continue;
				}
				if ((clock % 4) == 0) {
					prevChIndex = chIndex;
				} else {
					prevChIndex = chIndex + 64 * prevChIndex;
				}
				if ((clock++ % 4) == 0) {
					continue;
				}
				int shift = (-2 * clock) & 6;
				result += (char) (255 & (prevChIndex >> shift));
			}
			String q = "";
			for (int i = 0; i < result.length(); i++) {
				String alpha = "00" + Integer.toHexString(result.charAt(i));
				q += "%" + alpha.substring(alpha.length() - 2);
			}
			//System.out.println(q);
			return new String(result.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
		}

		public static String base64Encode(String x) {
			byte[] bs = x.getBytes(StandardCharsets.UTF_8);
			for (int i = 0; i < bs.length; i++) {
				System.out.print(" " + Integer.toHexString(bs[i] & 0xFF));
			}
			System.out.println();

			String res = Base64.getEncoder().encodeToString(bs);
			String newRes = "";
			for (int i = 0; i < res.length(); i++) {
				char c = res.charAt(i);
				if (c == '=') continue;
				if (Character.isUpperCase(c)) {
					newRes += Character.toLowerCase(c);
				} else {
					newRes += Character.toUpperCase(c);
				}
			}
			return newRes;
		}

		public static String WEMegZ(String a, String b) {
			a = base64Decode(a);

			int[] list = new int[256];
			for (int i = 0; i < 256; i++) {
				list[i] = i;
			}

			int acc = 0;
			for (int i = 0; i < 256; i++) {
				acc = (acc + list[i] + b.charAt(i % b.length())) % 256;

				int temp = list[i];
				list[i] = list[acc];
				list[acc] = temp;
			}

			int i = 0;
			acc = 0;

			String result = "";
			for (int j = 0; j < a.length(); j++) {
				i = (i + 1) % 256;
				acc = (acc + list[i]) % 256;

				int temp = list[i];
				list[i] = list[acc];
				list[acc] = temp;

				int ch = a.charAt(j) ^ list[(list[i] + list[acc]) % 256];
				result += (char) ch;
			}
			return result;
		}
	}

	public static String decodingFunction(int i, String s) {
		return Decoder.WEMegZ(theTable[i - 257], s);
	}

	public static void main4(String[] args) {
		String str = Decoder.base64Encode(args[0]);
		System.out.println(str);
		str = Decoder.WEMegZ(str, args[1]);
		System.out.println(str);
		for (int i = 0; i < str.length(); i++) {
			System.out.print(" " + Integer.toHexString(str.charAt(i)));
		}
		System.out.println();
		str = Decoder.base64Encode(str);
		System.out.println(str);
		str = Decoder.WEMegZ(str, args[1]);
		System.out.println(str);
	}

	public static int myParseInt(String s) {
		int i = 0;
		if (s.charAt(i) == '-' || s.charAt(i) == '+') i++;
		char c;
		while (i < s.length() && (c = s.charAt(i)) >= '0' && c <= '9') {
			i++;
		}
		return Integer.parseInt(s, 0, i, 10);
	}

	public static void main2(String[] args) {
		for (int i = 0; i < theTable.length; i++) {
			try {
				int a1 = myParseInt(decodingFunction(314, "c@Hn"));
				int a2 = myParseInt(decodingFunction(275, "OZos"));
				int a3 = myParseInt(decodingFunction(295, "p6OJ"));
				int a4 = myParseInt(decodingFunction(441, "!(Jy"));
				int a5 = myParseInt(decodingFunction(475, "c@Hn"));
				int a6 = myParseInt(decodingFunction(418, "Lvkz"));
				int a7 = myParseInt(decodingFunction(428, "cWDm"));
				int a8 = myParseInt(decodingFunction(545, "OZos"));
				int a9 = myParseInt(decodingFunction(669, "llA^"));
				int a10 = myParseInt(decodingFunction(480, "OZos"));
				System.err.println("Shift " + i);
				System.err.println(a1 + " " + a2 + " " + a3 + " " + a4 + " " + a5 + " " + a6 + " " + a7 + " " + a8 + " " + a9 + " " + a10);
				break;
			} catch (Exception e) {
			}
			shiftTable();
		}
	}

	public static void shiftTable() {
		String first = theTable[0];
		System.arraycopy(theTable, 1, theTable, 0, theTable.length - 1);
		theTable[theTable.length - 1] = first;
	}

	static String[] theTable = {"WPL9tgvD", "rSkQESkIxq", "W6ZcP8oaEhO", "W6mYb2bA", "dHDnimo6", "xmouaaldSa", "umk2ASk+Aa", "su4YWOxcGq", "tHddKCkblW", "W6JcI8oXD3O", "oejaWPXe", "ccnenei", "W5ZdH8o7W4i6", "W53cMhBcLSkQ", "uKODWPxcKa", "acj7oCo5", "W4PQmfyT", "wmk7W67dMmo9", "W4xcG8oBpI8", "dCk6xmoBwa", "W7JdRYGgbW", "W5XIAwOF", "WQpcTxiIDq", "qNJcRMhcOa", "W4NcU8oiW5rK", "W4/dK8oXW4G9", "W6NdSCkMW4Se", "W5eZdSk6va", "W4JdK8oFxau", "WOhdN13dNI8", "iSk9x8oJmb3dGSk5r8oTs8oLpG", "oMmxjcVdKHHa", "W6NcHCkta8oO", "WQmmgXldUW", "W6SSe2LB", "nCkBbfrA", "sImjdIy", "WQtdJmkmomkr", "mNRcIConFa", "W6iiba", "W7hdIbCbhq", "BmoAkZPG", "W5tdJ8oCW6Wr", "vK7dLKlcMW", "WOpdO8opWPWI", "vZisW78", "W6pcOmogCxK", "DSoLedZdHa", "ruGQ", "WRJdN8oMWRL+", "AZqthZe", "W7qMW6b/W6NcTLHAW7ysW6y", "WQpcVmkLWOb6", "WPZcUfiFvq", "gCkxBCoYDG", "p8oqWOtcG8kW", "EmoHArLS", "e03cVf5E", "WO3cPemhsG", "ACoCcMyc", "W7K5aNDb", "ytuA", "itjWd8of", "tmoFBqvd", "W7ddSmoOBGe", "xSkSxG", "ut7dLCkfkG", "x8k/wSopxG", "jvT/dmoZ", "CSoHCtnH", "bsxdIIddPHJcGmkDmCkeDW", "W6/cG8ksgmoU", "ae7cQSofFW", "zmk0W73dOhC", "bCkqy8oXCG", "tCkoW4pcH8ke", "WR7dI0ldTYW", "ax7cT8ooxG", "W5dcMCowsfK", "W7WPkwzd", "W5rPaeaU", "W6m6mHuI", "CdhdO8ktdW", "W67dLmoGW4mP", "WPpdKSkyWQSS", "W4JdJHWEfq", "tbNdLSkqoW", "nLnQn8or", "W6ddTmoLW4S0", "W7/dUSoJW7af", "W6uUdG", "ruS7WOJcSa", "u8o9WQvnuq", "vYGpqYm", "WQXix2nM", "f8oAWRhcRSkI", "FCo8WRddJSoj", "CZmLCHi", "a0nynCo7", "uCo2WQu", "rHuqWP5qkCkwWPiZ", "W6tdKmo7W64n", "cLH3WRL1", "cIjHoa", "W5JdISoXW7Cp", "W6ldMrKCha", "W63dHCoFsWm", "dhflu8k6", "WRVcVv0pxa", "WRjVWQqQ", "W4DAumksfG", "r8oCtXldSG", "u1q9", "xmkRCG", "ahhcOCoa", "uGRdVSkqiG", "B8oAeNKc", "W4nOASkJma", "f8oqWQddPCk5", "vmobka", "W7tcRCop", "rCkJsmkcrG", "FHK0gYm", "WRj5WOC7WQi", "W7u+aCksva", "W6z0Frq2", "vaiWFXi", "D08WW5xdTG", "WQumeXJdOG", "WQBcUCkVWQLe", "FSoSWPxdJCoo", "omkSw8onvW", "WPpdPmofWOy", "WOqJvsf5", "AaO6uX4", "kxfXmSor", "WOhdVmkyWOSK", "W6GqbCkrBG", "baqenSoJ", "nrrHlMO", "W4VcPmojwMe", "WOddN0RdMrO", "WOFdNSkaWPiO", "CCo9W5ZdJSoq", "W6tdImoIW4KF", "khnBs8oH", "W6yFlSkrtq", "tCoXdh40", "WPVdQmkWfSky", "mxTmWQbl", "uZGfW7hdMq", "l8kXzCotDa", "vb3dHSkuoG", "ytqt", "W6m9lvve", "zSoUWOBdNa", "CYqorrm", "DIWtW6ldHW", "kmkCdW", "WRe4WRmGWR8", "suG6WOpcVa", "W5FcLCoiht0", "d8kmiCo9Fa", "WO/cU15eqq", "W6zMESk+", "saqVzHC", "rCk6W7RdNSoO", "W6JcOK7cP8kc", "WPTZyhjK", "sSkdW6hdLNy", "yCk+W5BcMSkm", "wmoPf8kOyW", "WPC4DHDT", "DHekW4RdLa", "amoPaCkoeXhdRCo9WPBcLSouW6CL", "abvDj8o+", "WOVdKx4", "qmodcCk2tq", "WQRdKSoUWPn9", "jfDJa8ot", "qmoydSkPEG", "D8kYFSoptG", "smo5WONdSCoY", "FCoLomk6xa", "jCk5W5hcKmojWQtcIXxdG34", "W7/dSSocAIK", "W6uDjCkuFG", "qCkYuSocta", "vqBdUmknkG", "WQeCtGfQ", "W4PFcgGC", "WOVcKgaUEa", "WQT6W7SSWRG", "WRZdLCoMWR4", "FSk4Fmkata", "W5T5FZaa", "suaAWOtcGa", "W5JdRaCaqCkOWPhdSCoZ", "sYtdOSkhaq", "WQ7dGSoYWQSK", "W6OCl8kAqq", "t8knW7xdQmo3", "BCk4W6ldOCoi", "DmkBFCoDza", "n8kye0rp", "pmoJWPa", "WQztshXZ", "WOddM1hdQWS", "iejRWPPY", "ihLLWR9e", "BCojWOzkwa", "bXne", "i03cGhD4", "gWjBpSo9", "WO/dVCooWPC5", "WPVcL3e5ya", "WRLlAKzn", "mSkyAmoCsG", "WPJdKNldUI8", "WQZcTmkhph4vimkvWRddTgpcV8k4mq", "WRy1CrnW", "W7qPnbC4", "W7GGpW", "W43dJmoHsJa", "W5PdAvTc", "WQayAbDZ", "W53cJYtcUhGbuCkFW6LwW6y", "zZzsbde", "W4tdU8oIW6CI", "WRldNSoSWQ9Y", "DmkAe19s", "bcBdJsldOx7dVCk7lCkezLDh", "WO5YuvPd", "WRWBBGji", "W6/dRmoNW4mc", "W6ficMGk", "oM1ix3ZcJLO+WPC6W67dLsFcKa", "WR3dUSoNWQjT", "BGmQbJu", "xCoKp38z", "W71GtZ0d", "lSkcW4hdHCot", "yCoUjSknBW", "hgtcOCos", "f3lcKSoTBq", "WQtcKmk1WRbi", "W7Prz8k7oq", "WQtcJSku", "qSkyW6/dP1a", "rceNW6ddSa", "wSolDqTX", "zSk4WRtdOxy", "DmoKamkWyq", "W7ZcJuFcUSki", "c8koF8oYAG", "D8kyW5FcN8oy", "FSoTDJn2", "bYrRp3K", "W6/dUCoBW4Ou", "sIK7", "b8kNrCopAW", "Emo/eHhdUa", "qXOwW40BE8onWReBrZ3dR8kz", "WO7dGSoRWQbO", "tI8BW6JdTa", "W7W5W7SKWQu", "W6q4adeJ", "WRGkvq5+", "rCoqfXhdOa", "CSo9Ca", "BtRdH8kHoa", "WR9aA2zs", "t8koW6hdUmo6", "rSoFbrxdQW", "W5bvegC8", "sHpdO8kboW", "WRjft0Dc", "rSk6zmoXxa", "y8oKhmkZ", "WOxcULCosW", "gt/dLd3dVG", "DCkqB8k2Bq", "q2BdIfBcKG", "FXG/pHy", "W6FdM8o9FXC", "bhRcPCoaFG", "WQxcKmo4WR5o", "WQFdHmkxEwi", "WQhcKCkX", "W601bIHd", "dsrfmSoG", "W4ZdTCo8rXe", "sMm4WRlcHq", "vCoTWQrABW", "aq9nmSo1", "WRFdUCkpWPSI", "vYqXW67dHW", "W67dPCofW58+", "WRSeAqzU", "WR0rDa9P", "vxdcLgtcSG", "p21npXhdRaT+WRu", "rvldQ3BcTW", "W7uGmHqG", "sMldJepcJW", "bKHi", "vCoOWQfKra", "WOtcOeCBqa", "vNpdGeFdKW", "W48giNXw", "u8oWf2CM", "CLeqWQ7cKG", "vSk7r8o+rq", "W6NcRmovycG", "qc85xYy", "WPtdQ8kKfmkf", "FSobkMSw", "cW5e", "WRaFCH10", "WRNdOmkPWPW8", "B33cMq", "umoxWRRcPG", "W4iDdMTn", "W6jJsXyi", "t1qZ", "W7TynvG2", "WQVdNmoPWR5S", "umotWRRcPmk9", "W4GykKfy", "W5NdVYmWoW", "W6PkyCkJmG", "khRdRx9+", "W5fOb0qI", "W6FcRCoxxgi", "cYLKowa", "z8kRxSkGEq", "WPTwAW", "xSoqlMOZ", "WPldSCkpWPSI", "v8o3WR5Jva", "W6BcJ8k6WRbj", "x2BcKa", "WQhcKCkYWRre", "tSoefcldGq", "WOeKxdX7", "W5LIdKG0", "lbbnleO", "h8o6WPRcJCkd", "WO7dTmkacmka", "AZWpnaG", "jgpdVhPJ", "WOtdSCkXbCkH", "W4j7EIeE", "lSkZfNvj", "tYiOvJy", "D2ddV3FcVG", "WPNdO8o4WPPZ", "b3pcPCogEW", "W4XNrcK8", "vaiRW5NdNq", "jh1nc8o2", "W7eMW6z3WRVcU3fwW6eM", "o8oLWR3cSYyIfCoRggpdTW", "W6qAgN1A", "nHjUeNm", "W6HLzSkLla", "b2r/pCoT", "W6VdH8oBW60G", "dsO9pNe", "W5xdJ8ouAWG", "jWnrjmoM", "As4ipHC", "w8k8FSk0", "wCobjrZdTW", "umkiW7BdU8oS", "WPfUWQmLWPe", "W6iFmwTw", "saBdP8kNeW", "W7Pve3eJ", "W6tdTbqzfa", "W4xdQmogW5qz", "WRz0WRmOWQy", "ymoQWPb4wa", "hCoBWRJdPmkO", "nxvqeCkT", "WRWofGNdVa", "lMjf", "WOXJxKjD", "cXnmnSo5", "FSohfIRdHa", "r8odbby", "WOf8vezT", "wCkSvSoq", "dCkxASkZCG", "ehpcT8oFuq", "W7dcPmocBwG", "BCkdWO/dKSou", "xNNcMh3cSa", "WPVdVgRdMIG", "suKW", "o2lcS21Q", "W40Lj8kTtG", "WQJdMmo9W6rP", "WQhcTvaIxq", "FsKhdwS", "WPeJBszK", "W6LnwSkpfq", "cv3cOKL1", "zCk6W6RdOMS", "jSk7W5pcNmkuW7JdIYddNhn4xcK", "nwvZWObu", "cCk/cNfz", "W6ddTCoV", "ymk6W6C", "W5ekbmk1uW", "ECo5WRjHuW", "WRZcP8k4WQbB", "CCo7WORdMmow", "udu/", "F8obm0Gd", "WOldOSkKWQC7", "gCkGohvU", "W6DVtCkOgG", "DSoyWPxdOSoe", "EZGpbtu", "WQr5yvL1ksf0W63cRalcUKa", "oCklffvC", "Amobf21ghNCp", "udOvlW4", "owrEvmkS", "W5hdVmoMW6Wv", "W4lcP0CkrW", "wJujgqS", "o8kmchD6", "W6VcQCk0lmos", "W4tcHCoEiZW", "BmktW4ddH2W", "W7FcJCohkSkE", "z8o0bWhdOa", "WRRdP8kmCNS", "W7VdPmoxEti", "uwuDWQRcLq", "W6ddRSoQW506", "WRzbWRCdWRW", "DCotfSkzqa", "W6SalSkz", "m17cK8obBG", "CsKOqH8", "A8kyB8kazG", "WP1hDrPg", "g0naWPbA", "WQmxaXJdQa", "rmolpSkWFW", "W6ZcNSoAnJ0", "rhVcJN4", "WPddVmkaWOSR", "W7NdSCo7", "ACkbW4tdLmoe", "W7hdSmoF", "yrqSzYG", "WRRdP8kmBNK", "W6NcPSoe"};
}
