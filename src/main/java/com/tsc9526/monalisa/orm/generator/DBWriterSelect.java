/*******************************************************************************************
 *	Copyright (c) 2016, zzg.zhou(11039850@qq.com)
 * 
 *   Monalisa is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU Lesser General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.

 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU Lesser General Public License for more details.

 *	You should have received a copy of the GNU Lesser General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************************/
package com.tsc9526.monalisa.orm.generator;


import java.io.PrintWriter;
import com.tsc9526.monalisa.tools.template.jsp.JspContext;
import com.tsc9526.monalisa.orm.Version;
import com.tsc9526.monalisa.orm.meta.MetaTable;
import com.tsc9526.monalisa.orm.meta.MetaColumn;
import java.util.Set;

/**
 * Auto generate code from jsp: src\main\resources\com\tsc9526\monalisa\orm\resources\template\select.jsp
 * 
 * @author zzg.zhou(11039850@qq.com)  
 */

public class DBWriterSelect{
	
	String toComments(String remarks){
		return remarks==null?"": remarks.replace("*/","**");
	}
	
	String toJavaString(String s){
		if(s==null)return "";
		
		return s.trim().replace("\"","\\\"").replace("\r","\\r").replace("\n","\\n");
	}
 
	String getComments(MetaTable table,MetaColumn c,String params,String leftPadding){
		String cname=c.getName();
		
		if(cname!=null && cname.length()>0 && c.getTable()!=null){	
			String r="/**\r\n"+leftPadding;
			r+="* @Column\r\n"+leftPadding; 
			r+="* <li>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<B>table:</B> "+c.getTable().getName()+"&nbsp;<B>name:</B> "+cname;
			
			if(c.isKey() || c.isAuto() || c.isNotnull() || c.isEnum()){
				boolean b=false;
				r+=" &nbsp;[";
				if(c.isKey()){
					r+=(b?"|":"")+"<font color=red>KEY</font>";
					b=true;
				}
				if(c.isAuto()){
					r+=(b?"|":"")+"<font color=red>AUTO</font>";
					b=true;
				}
				if(c.isNotnull()){
					r+=(b?"|":"")+"<font color=red>NOTNULL</font>";
					b=true;
				}
				if(c.isEnum()){
					r+=(b?"|":"")+"<font color=red>ENUM</font>";
					b=true;
				}
				r+="]";
			}
			r+="\r\n"+leftPadding;
			
			if(c.getLength()>0 || c.getValue()!=null){
				r+="* <li>&nbsp;&nbsp;&nbsp;";
			
				if(c.getLength()>0){
					r+="<B>length:</B> "+c.getLength();
				}
				if(c.getValue()!=null){
					r+=" &nbsp;<B>value:</B> "+toJavaString(c.getValue());
				}
				r+="<br>\r\n"+leftPadding;
			}
			
			if(c.getRemarks()!=null){
				r+="* <li><B>remarks:</B> "+toComments(c.getRemarks())+"\r\n"+leftPadding;
			}
			 
			if(params==null){
				params="";
			}
			params=params.trim();
			if(params.length()>0){
				r+="* "+params;
			}
			
		 	r+="*/\r\n"+leftPadding;	
		 
		 	String f=c.getTable().getJavaName()+".M.";
		 	if(c.getTable().getJavaPackage().equals(table.getJavaPackage())){
		 		f="M.";
		 	}
			 	
			String[] names=new String[]{"name","key","auto","seq","notnull","length","decimalDigits","value","remarks"};
			
			r+="@Column(table="+f+"TABLE, jdbcType="+c.getJdbcType();
			for(String n:names){
				String colname=c.nameToJava();
				int p=colname.indexOf("$");
				if(p>0){
					colname=colname.substring(p+1);
				}
			
				r+=", "+n+"="+f+colname+"$"+n;
			}
			r+=")";
			 
			return r;
		}else{
			return "";
		}
	}

	String firstUpper(String s){
		return s.substring(0,1).toUpperCase()+s.substring(1);
	}

	String html(Object v){
		if(v==null){
			return "";
		}else{
			return v.toString().trim();
		}
	}

	public void service(JspContext request, PrintWriter out) {
		MetaTable table = (MetaTable) request.getAttribute("table");
		@SuppressWarnings("unchecked")
		Set<String> imports = (Set<String>) request.getAttribute("imports");
		String fingerprint = (String) request.getAttribute("fingerprint");
		String see = (String) request.getAttribute("see");

		StringBuilder output = new StringBuilder();

		output.append("package ").append(table.getJavaPackage()).append(";\n\n");

		boolean shouldAddImports = false;
		for (MetaColumn c : table.getColumns()) {
			if (c.getTable() != null && c.getCode("file") != null) {
				imports.add("java.io.File");
				imports.add("com.tsc9526.monalisa.tools.io.MelpFile");
				imports.add("com.tsc9526.monalisa.orm.datasource.DBConfig");
				shouldAddImports = true;
				break;
			}
		}

		if (shouldAddImports) {
			for (String i : imports) {
				output.append("import ").append(i).append(";\n");
			}
			output.append("\n");
		}

		output.append("/**\n");
		output.append(" * Auto generated code by monalisa ").append(Version.getVersion()).append("\n");
		output.append(" *\n");
		output.append(" * @see ").append(see).append("\n");
		output.append(" */\n");
		output.append("public class ").append(table.getJavaName()).append(" implements java.io.Serializable {\n");
		output.append("    private static final long serialVersionUID = ").append(table.getSerialID()).append("L;\n");
		output.append("    final static String FINGERPRINT = \"").append(fingerprint).append("\";\n\n");

		for (MetaColumn f : table.getColumns()) {
			output.append(getComments(table, f, "\t", "\t")).append("\n");
			output.append("    private ").append(f.getJavaType()).append(" ").append(f.getJavaName()).append(";\n\n");
		}

		for (MetaColumn f : table.getColumns()) {
			output.append(getComments(table, f, "\t", "\t")).append("\n");
			output.append("    public ").append(table.getJavaName()).append(" ").append(f.getJavaNameSet())
					.append("(").append(f.getJavaType()).append(" ").append(f.getJavaName()).append(") {\n");
			output.append("        this.").append(f.getJavaName()).append(" = ").append(f.getJavaName()).append(";\n");
			output.append("        return this;\n");
			output.append("    }\n\n");
		}

		for (MetaColumn f : table.getColumns()) {
			output.append(getComments(table, f, "\t", "\t")).append("\n");
			output.append("    public ").append(f.getJavaType()).append(" ").append(f.getJavaNameGet()).append("() {\n");
			output.append("        return this.").append(f.getJavaName()).append(";\n");
			output.append("    }\n\n");

			output.append(getComments(table, f, "@param defaultValue  Return the default value if ")
					.append(f.getJavaName()).append(" is null.", "\t")).append("\n");
			output.append("    public ").append(f.getJavaType()).append(" ").append(f.getJavaNameGet())
					.append("(").append(f.getJavaType()).append(" defaultValue) {\n");
			output.append("        ").append(f.getJavaType()).append(" r = this.").append(f.getJavaNameGet()).append("();\n");
			output.append("        if (r == null) {\n");
			output.append("            r = defaultValue;\n");
			output.append("        }\n\n");
			output.append("        return r;\n");
			output.append("    }\n\n");

			String file = f.getCode("file");
			if (f.getTable() != null && file != null) {
				output.append(getComments(table, f, "@param charset  read file content using this charset.", "\t")).append("\n");
				output.append("    public String ").append(f.getJavaNameGet()).append("AsString(String charset) {\n");
				output.append("        ").append(f.getJavaType()).append(" r = this.").append(f.getJavaNameGet()).append("();\n");
				output.append("        if (r == null) {\n");
				output.append("            return null;\n");
				output.append("        }\n\n");
				output.append("        DBConfig db = DBConfig.fromClass(").append(f.getTable().getJavaName()).append(".class);\n");
				output.append("        String filepath = MelpFile.combinePath(\"").append(file).append("\", r);\n");
				output.append("        filepath = db.getCfg().parseFilePath(filepath);\n");
				output.append("        return MelpFile.readToString(new File(filepath), charset);\n");
				output.append("    }\n\n");

				output.append(getComments(table, f, "\t", "\t")).append("\n");
				output.append("    public String ").append(f.getJavaNameGet()).append("AsStringUTF8() {\n");
				output.append("        return ").append(f.getJavaNameGet()).append("AsString(\"utf-8\");\n");
				output.append("    }\n\n");

				output.append(getComments(table, f, "\t", "\t")).append("\n");
				output.append("    public byte[] ").append(f.getJavaNameGet()).append("AsBytes() {\n");
				output.append("        ").append(f.getJavaType()).append(" r = this.").append(f.getJavaNameGet()).append("();\n");
				output.append("        if (r == null) {\n");
				output.append("            return null;\n");
				output.append("        }\n\n");
				output.append("        DBConfig db = DBConfig.fromClass(").append(f.getTable().getJavaName()).append(".class);\n");
				output.append("        String filepath = MelpFile.combinePath(\"").append(file).append("\", r);\n");
				output.append("        filepath = db.getCfg().parseFilePath(filepath);\n");
				output.append("        return MelpFile.readFile(new File(filepath));\n");
				output.append("    }\n\n");

				output.append("    public File ").append(f.getJavaNameGet()).append("AsFile() {\n");
				output.append("        ").append(f.getJavaType()).append(" r = this.").append(f.getJavaNameGet()).append("();\n");
				output.append("        if (r == null) {\n");
				output.append("            return null;\n");
				output.append("        }\n\n");
				output.append("        DBConfig db = DBConfig.fromClass(").append(f.getTable().getJavaName()).append(".class);\n");
				output.append("        String filepath = MelpFile.combinePath(\"").append(file).append("\", r);\n");
				output.append("        filepath = db.getCfg

			}