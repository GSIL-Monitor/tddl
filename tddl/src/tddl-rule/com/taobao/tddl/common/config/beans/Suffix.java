package com.taobao.tddl.common.config.beans;


/**
 * һ��suffix 
 * @author liang.chenl
 *
 */
public class Suffix {
	
	/**
	 * ����ֵ��tbSuffix�������������Ҫ��ʽ����
	 */
	private int tbNumForEachDb; //��tbsuffix��dbIndexes�����������
	private int tbSuffixFrom = 0;
	private int tbSuffixTo = -1;
	private int tbSuffixWidth = 4;
	private String tbSuffixPadding = "_";
	private String tbType;
	
	public int getTbNumForEachDb() {
		return tbNumForEachDb;
	}
	public void setTbNumForEachDb(int tbNumForEachDb) {
		this.tbNumForEachDb = tbNumForEachDb;
	}
	public int getTbSuffixFrom() {
		return tbSuffixFrom;
	}
	public void setTbSuffixFrom(int tbSuffixFrom) {
		this.tbSuffixFrom = tbSuffixFrom;
	}
	public int getTbSuffixTo() {
		return tbSuffixTo;
	}
	public void setTbSuffixTo(int tbSuffixTo) {
		this.tbSuffixTo = tbSuffixTo;
	}
	public int getTbSuffixWidth() {
		return tbSuffixWidth;
	}
	public void setTbSuffixWidth(int tbSuffixWidth) {
		this.tbSuffixWidth = tbSuffixWidth;
	}
	public String getTbSuffixPadding() {
		return tbSuffixPadding;
	}
	public void setTbSuffixPadding(String tbSuffixPadding) {
		this.tbSuffixPadding = tbSuffixPadding;
	}

	public void setTbSuffixTo(String[] dbIndexes) {
		this.tbSuffixTo = dbIndexes.length - 1 + this.tbSuffixFrom;
	}
	public String getTbType() {
		return tbType;
	}
	public void setTbType(String tbType) {
		this.tbType = tbType;
	}

}