package com.taobao.tddl.rule.le.topology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * ����һ���й��ɵ������б�
 * 
 * <pre>
 *   NamePattern = Name | ( Prefix SuffixExpr )
 *   
 *   SuffixExpr = "[" NameSuffix *( "," NameSuffix ) "]"
 *   
 *   NameSuffix = Pattern | NameRange
 *   
 *   NameRange = Min "-" Max
 * </pre>
 * 
 * @author changyuan.lh
 */
public final class NamePattern extends NameSuffix {

    private String prefix;

    private List<NameSuffix> suffixExpr;

    public NamePattern(String prefix) {
	this(prefix, null);
    }

    public NamePattern(String prefix, List<NameSuffix> nameRange) {
	this.suffixExpr = nameRange;
	this.prefix = prefix;
    }

    public String getPrefix() {
	return prefix;
    }

    public List<NameSuffix> listSuffix() {
	return suffixExpr;
    }

    final void addSuffix(String suffix) {
	if (NameRange.numericCheck(suffix)) {
	    NameRange nameRange = new NameRange(suffix);
	    addSuffix(nameRange);
	} else {
	    NamePattern namePattern = new NamePattern(suffix);
	    addSuffix(namePattern);
	}
    }

    final void addSuffix(NameSuffix nameSuffix) {
	if (suffixExpr == null) {
	    suffixExpr = new ArrayList<NameSuffix>();
	}
	suffixExpr.add(nameSuffix);
    }

    // ����ƥ���ǰ׺����
    private int matchPrefix(String name) {
	final int len = name.length();
	boolean underline = false;
	boolean number = false;
	int i = 0, match = 0;
	for (; i < prefix.length(); i++) {
	    char ch = prefix.charAt(i);
	    // ���ǰ׺�Ƿ�ƥ��
	    if (i >= len) {
		return match;
	    }
	    // �������ֺ��»��߷ֶ�
	    if (ch == '_') {
		underline = true;
		number = false;
	    } else if (Character.isDigit(ch)) {
		// ���ֵ�����һ��
		if (!number) {
		    match = i;
		}
		underline = false;
		number = true;
	    } else {
		// �»��߽�����һ��
		if (underline) {
		    match = i;
		}
		underline = false;
		number = false;
	    }
	    // ���ǰ׺�Ƿ�ƥ��
	    if (ch != name.charAt(i)) {
		return match;
	    }
	}
	// �����������ƥ��ǰ׺, ��Ҫ���⴦��
	if (i == len && suffixExpr != null) {
	    return match;
	}
	return i;
    }

    // ��ǰ׺λ�úϲ���֧��׺
    private void branchPrefix(String suffix, int fromIndex) {
	String branch = prefix.substring(fromIndex);
	prefix = prefix.substring(0, fromIndex);
	if (suffixExpr != null) {
	    List<NameSuffix> suffixList = new ArrayList<NameSuffix>();
	    suffixList.add(new NamePattern(branch, suffixExpr));
	    suffixExpr = suffixList;
	} else {
	    addSuffix(branch);
	}
	addSuffix(suffix);
    }

    // �������͵ĺ�׺�ϲ�
    private void mergeRange(String name) {
	final int len = name.length();
	final long number = Long.parseLong(name);
	final int zeroPadding = (name.charAt(0) == '0') ? len : 0;
	NameRange mergeRange = null;
	for (NameSuffix nameSuffix : suffixExpr) {
	    if (nameSuffix instanceof NameRange) {
		NameRange nameRange = (NameRange) nameSuffix;
		if (nameRange.put(number, zeroPadding)) {
		    mergeRange = nameRange;
		    break;
		}
	    }
	}
	if (mergeRange != null) {
	    Iterator<NameSuffix> it = suffixExpr.iterator();
	    while (it.hasNext()) {
		NameSuffix nameSuffix = it.next();
		if (nameSuffix instanceof NameRange) {
		    NameRange nameRange = (NameRange) nameSuffix;
		    if (nameRange != mergeRange) {
			if (mergeRange.merge(nameRange)) {
			    it.remove();
			}
		    }
		}
	    }
	} else {
	    NameRange nameRange = new NameRange(number, number, zeroPadding);
	    suffixExpr.add(nameRange);
	}
    }

    // �ϲ�һ������������
    protected boolean merge(String name) {
	// ǰ׺ƥ�����֧�ϲ�
	if (prefix != null) {
	    final int match = matchPrefix(name);
	    if (match == 0) {
		// ǰ׺��ȫ��ƥ��
		return false;
	    } else if (match < prefix.length()) {
		// ��Ϊһ���·�֧����, ���úϲ���׺
		branchPrefix(name.substring(match), match);
		return true;
	    }
	    name = name.substring(match);
	}

	// ��׺ƥ��ͺϲ�
	if (suffixExpr != null) {
	    if (NameRange.numericCheck(name)) {
		// �������͵ĺ�׺�ϲ�
		mergeRange(name);
	    } else {
		// �ַ����͵ĺ�׺�ϲ�
		for (NameSuffix nameSuffix : suffixExpr) {
		    if (nameSuffix instanceof NamePattern) {
			NamePattern namePattern = (NamePattern) nameSuffix;
			if (namePattern.merge(name)) {
			    return true;
			}
		    }
		}
		// �����ĺ�׺
		suffixExpr.add(new NamePattern(name));
	    }
	    return true;
	}

	// û��ƥ��ĺ�׺
	return name.isEmpty();
    }

    // �ϲ�һ����������������
    public static NamePattern merge(NamePattern namePattern,
	    Collection<String> names) {
	for (String name : names) {
	    // ��������Ŀո�
	    name = name.trim();
	    if (namePattern == null) {
		if (NameRange.numericCheck(name)) {
		    namePattern = new NamePattern(null,
			    new ArrayList<NameSuffix>());
		    NameRange nameRange = new NameRange(name);
		    namePattern.addSuffix(nameRange);
		} else {
		    namePattern = new NamePattern(name);
		}
	    } else if (!namePattern.merge(name)) {
		NamePattern mergePattern = new NamePattern(null,
			new ArrayList<NameSuffix>());
		mergePattern.addSuffix(namePattern);
		mergePattern.addSuffix(name);
		namePattern = mergePattern;
	    }
	}
	return namePattern;
    }

    // �ϲ�һ����������������
    public static NamePattern merge(NamePattern namePattern, String... names) {
	return merge(namePattern, Arrays.asList(names));
    }

    // �ϲ�һ����������������
    public static NamePattern merge(Collection<String> names) {
	return merge(null, names);
    }

    // �ϲ�һ����������������
    public static NamePattern merge(String... names) {
	return merge(Arrays.asList(names));
    }

    public boolean contains(String name) {
	if (prefix != null) {
	    if (!name.startsWith(prefix)) {
		return false;
	    }
	    name = name.substring(prefix.length());
	}

	if (suffixExpr != null) {
	    for (NameSuffix nameSuffix : suffixExpr) {
		if (nameSuffix.contains(name))
		    return true;
	    }
	    return false;
	} else {
	    return name.isEmpty();
	}
    }

    protected List<String> iterate(StringBuilder buf, List<String> list) {
	if (prefix != null)
	    buf.append(prefix);

	if (suffixExpr == null) {
	    list.add(buf.toString());
	} else {
	    final int buflen = buf.length();
	    for (NameSuffix nameSuffix : suffixExpr) {
		nameSuffix.iterate(buf, list);
		buf.setLength(buflen);
	    }
	}
	return list;
    }

    final static void escape(StringBuilder buf, String name) {
	final int len = name.length();
	for (int i = 0; i < len; i++) {
	    char ch = name.charAt(i);
	    if (ch == '\\' || ch == '-') {
		buf.append('\\');
	    }
	    buf.append(ch);
	}
    }

    final static String unescape(String input) {
	final int len = input.length();
	StringBuilder buf = null;
	int index = 0;
	for (int i = 0; i < len; i++) {
	    char ch = input.charAt(i);
	    if (ch == '\\') {
		if (buf == null) {
		    buf = new StringBuilder(len);
		}
		buf.append(input.substring(index, i));
		index = i + 1;
		i ++;
	    }
	}
	if (buf != null) {
	    buf.append(input.substring(index));
	    return buf.toString();
	} else {
	    return input;
	}
    }

    /**
     * SuffixExpr = "[" NameSuffix *( "," NameSuffix ) "]"
     * 
     * NameSuffix = Pattern | NameRange
     */
    protected int loadSuffix(String input, int fromIndex) {
	final int len = input.length();
	int index = fromIndex;
	int minusIndex = -1;
	for (int i = index; i < len; i++) {
	    char ch = input.charAt(i);
	    switch (ch) {
	    case '\\':
		i++; // ����ת���ַ�
		break;
	    case '-':
		minusIndex = i;
		break;
	    case '[': {
		String prefix = input.substring(index, i).trim();
		NamePattern namePattern = new NamePattern(
			prefix.isEmpty() ? null : NamePattern.unescape(prefix),
			new ArrayList<NameSuffix>());
		index = namePattern.loadSuffix(input, i + 1);
		// TODO: Ӧ���Ƕ���: input.charAt(index)
		suffixExpr.add(namePattern);
		minusIndex = -1;
		i = index - 1;
		break;
	    }
	    case ',':
		if (i > index) {
		    String name = input.substring(index, i).trim();
		    if (!name.isEmpty()) {
			if (minusIndex > index || NameRange.numericCheck(name)) {
			    NameRange nameRange = NameRange.loadInput(name);
			    suffixExpr.add(nameRange);
			} else {
			    name = NamePattern.unescape(name);
			    NamePattern namePattern = new NamePattern(name);
			    suffixExpr.add(namePattern);
			}
		    }
		}
		minusIndex = -1;
		index = i + 1;
		break;
	    case ']':
		if (i > index) {
		    String name = input.substring(index, i).trim();
		    if (!name.isEmpty()) {
			if (minusIndex > index || NameRange.numericCheck(name)) {
			    NameRange nameRange = NameRange.loadInput(name);
			    suffixExpr.add(nameRange);
			} else {
			    name = NamePattern.unescape(name);
			    NamePattern namePattern = new NamePattern(name);
			    suffixExpr.add(namePattern);
			}
		    }
		}
		return i + 1;
	    }
	}

	if (len > index) {
	    String name = input.substring(index).trim();
	    if (!name.isEmpty()) {
		if (minusIndex > index || NameRange.numericCheck(name)) {
		    NameRange nameRange = NameRange.loadInput(name);
		    suffixExpr.add(nameRange);
		} else {
		    name = NamePattern.unescape(name);
		    NamePattern namePattern = new NamePattern(name);
		    suffixExpr.add(namePattern);
		}
	    }
	}
	return len;
    }

    /**
     * NamePattern = Name | ( Prefix SuffixExpr )
     * 
     * SuffixExpr = "[" NameSuffix *( "," NameSuffix ) "]"
     */
    public static NamePattern loadInput(String input) {
	final int bracketIndex = input.indexOf('[', 0);
	if (bracketIndex >= 0) {
	    String prefix = input.substring(0, bracketIndex).trim();
	    NamePattern namePattern = new NamePattern(prefix.isEmpty() ? null
		    : NamePattern.unescape(prefix), new ArrayList<NameSuffix>());
	    namePattern.loadSuffix(input, bracketIndex + 1);
	    return namePattern;
	} else {
	    input = NamePattern.unescape(input.trim());
	    return new NamePattern(input, null);
	}
    }

    protected StringBuilder buildString(StringBuilder buf) {
	if (prefix != null) {
	    // ����ת���ַ�
	    escape(buf, prefix);
	}
	if (suffixExpr != null) {
	    buf.append('[');
	    boolean comma = false;
	    for (NameSuffix nameSuffix : suffixExpr) {
		if (comma)
		    buf.append(',');
		nameSuffix.buildString(buf);
		comma = true;
	    }
	    buf.append(']');
	}
	return buf;
    }

    public static void main(String[] args) {
	NamePattern namePattern = NamePattern.loadInput("trade_[016-031]");
	System.out.println("Pattern: " + namePattern);
	System.out.println("  contains trade_017: "
		+ namePattern.contains("trade_017"));
	System.out.println("  contains trade_047: "
		+ namePattern.contains("trade_047"));
	System.out.println("  contains trade_1: "
		+ namePattern.contains("trade"));
	List<String> nameList = namePattern.list();
	System.out.println("list: " + Arrays.toString(nameList.toArray()));
	Collections.shuffle(nameList);
	System.out.println("merge: " + NamePattern.merge(nameList));
	System.out.println();

	NamePattern complexPattern = NamePattern
		.loadInput("tf_x_[01_[01-31], 02_[01-28], 03_[01-30]]");
	System.out.println("Complex Pattern: " + complexPattern);
	System.out.println("  contains tf_x_01_01: "
		+ complexPattern.contains("tf_x_01_01"));
	System.out.println("  contains tf_x_02: "
		+ complexPattern.contains("tf_x_02"));
	System.out.println("  contains tf_x_03_31: "
		+ complexPattern.contains("tf_x_03_31"));
	System.out.println("list: "
		+ Arrays.toString(complexPattern.list().toArray()));
	System.out.println("merge: " // NL
		+ NamePattern.merge(complexPattern.list()));
	System.out.println();

	NamePattern simplePattern = NamePattern
		.loadInput("[tf_01_[a, b], tf_02_[a, b]]");
	System.out.println("Simple Pattern: " + simplePattern);
	System.out.println("  contains tf_01_a: "
		+ simplePattern.contains("tf_01_a"));
	System.out.println("  contains tf_02_b: "
		+ simplePattern.contains("tf_02_b"));
	System.out.println("  contains tf_02_c: "
		+ simplePattern.contains("tf_02_c"));
	System.out.println("list: "
		+ Arrays.toString(simplePattern.list().toArray()));
	System.out.println("merge: " // NL
		+ NamePattern.merge(simplePattern.list()));
	System.out.println();

	System.out.println("Complex merge 1: ");
	String[] namelist1 = { "tf", "tf_", "tf_01", "tf_02", "tf_02_03",
		"tf_02_04", "tf_02_0a", "tf_02_0b" };
	System.out.println("from: " + Arrays.toString(namelist1));
	NamePattern mergePattern1 = NamePattern.merge(Arrays.asList(namelist1));
	System.out.println("pattern: " + mergePattern1);
	System.out.println("list: " // NL
		+ Arrays.toString(mergePattern1.list().toArray()));
	System.out.println();

	System.out.println("Complex merge 2: ");
	String[] namelist2 = { "crm", "crm1", "crm2", "crm3", "crm4", "crm01",
		"crm02", "crm03", "crm04", "crm_1", "crm_2", "crm_3", "crm_4",
		"crm11", "crm12", "crm13", "crm14" };
	System.out.println("from: " + Arrays.toString(namelist2));
	NamePattern mergePattern2 = NamePattern.merge(Arrays.asList(namelist2));
	System.out.println("pattern: " + mergePattern2);
	System.out.println("list: " // NL
		+ Arrays.toString(mergePattern2.list().toArray()));
	System.out.println();
    }
}
