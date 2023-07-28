/*******************************************************************************************
 *	Copyright (c) 2016, zzg.zhou(11039850@qq.com)
 * 
 *  Monalisa is free software: you can redistribute it and/or modify
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
package com.tsc9526.monalisa.orm.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.tsc9526.monalisa.orm.annotation.Column;
import com.tsc9526.monalisa.orm.annotation.DB;
import com.tsc9526.monalisa.orm.annotation.Index;
import com.tsc9526.monalisa.orm.annotation.Table;
import com.tsc9526.monalisa.orm.datasource.DBConfig;
import com.tsc9526.monalisa.orm.datasource.DbProp;
import com.tsc9526.monalisa.orm.dialect.Dialect;
import com.tsc9526.monalisa.orm.meta.MetaColumn;
import com.tsc9526.monalisa.orm.meta.MetaIndex;
import com.tsc9526.monalisa.orm.meta.MetaPartition;
import com.tsc9526.monalisa.orm.meta.MetaTable;
import com.tsc9526.monalisa.orm.utils.TableHelper;
import com.tsc9526.monalisa.tools.Tasks;
import com.tsc9526.monalisa.tools.clazz.MelpClass;
import com.tsc9526.monalisa.tools.clazz.MelpClass.ClassHelper;
import com.tsc9526.monalisa.tools.clazz.MelpClass.FGS;
import com.tsc9526.monalisa.tools.logger.Logger;
import com.tsc9526.monalisa.tools.string.MelpString;
import com.tsc9526.monalisa.tools.validator.Validator;
 
/**
 * 
 * @author zzg.zhou(11039850@qq.com)
 */
public class ModelMeta{	
	static Logger logger=Logger.getLogger(ModelMeta.class.getName());
	
	private static boolean modelReloadRunning=false;
	private static Map<String, ModelMeta> hMonitorMetas=new ConcurrentHashMap<String, ModelMeta>();
	private static Map<String, ModelMeta> hMetas       =new ConcurrentHashMap<String, ModelMeta>();
	
	public static ModelMeta getModelMeta(Model<?> model){
		String key=getModelKey(model);
		
		ModelMeta mm=hMetas.get(key);
	 	if(mm==null || mm.isChanged()){
			mm=createModelMeta(model,key);
		}
	 	
 	 	return mm;
	}
	
	private static ModelMeta createModelMeta(Model<?> model,String key){
		synchronized(model.getClass()){
			ModelMeta mm=hMetas.get(key);
		 	if(mm!=null && !mm.isChanged()){
		 		return mm;
		 	}
		 	 
			mm=new ModelMeta(key);
			mm.init(model);
			
			if(mm.record){
				if(!hMonitorMetas.containsKey(key)){
					logger.info("Add dynamic table: "+mm.tableName+", dbkey: "+mm.db.getKey());
				}
				hMonitorMetas.put(key, mm);
				
				if(!modelReloadRunning){
					modelReloadRunning=true;
					startReloadModelMetas();
				}
			}
			hMetas.put(key, mm);
		 	 
			return mm;
		}
	}
	
	private static void startReloadModelMetas(){
		int interval=DbProp.CFG_RELOAD_MODEL_INTERVAL;
		if(interval>0){
			Tasks.instance.addSchedule("ModelChangeTask", new TimerTask() {
				public void run() {
					reloadModelMetas();
				}
			}, interval*1000, interval*1000);
		}
	}
	 
	public synchronized static void reloadModelMetas(){
		//check if data source changed
		for(ModelMeta mm:hMonitorMetas.values()){
			mm.db.getDataSource();
		}
		
		for(ModelMeta mm:hMonitorMetas.values()){
			mm.checkChanged();
		} 
	}
	
	public synchronized static void clearReloadModelMetas(String dbKey){
		List<String> rms=new ArrayList<String>();
		for(String key:hMonitorMetas.keySet()){
			ModelMeta mm=hMonitorMetas.get(key);
			if(dbKey.equals( mm.db.getKey() )){
				rms.add(key);
			} 
		}
		
		if(rms.size()>0){
			logger.info("Remove dynamic tables: "+rms.toString()+", dbkey: "+dbKey);
			for(String key:rms){
				hMonitorMetas.remove(key);
				hMetas.remove(key);
			}
		}
	}
	
	private static String getModelKey(Model<?> model){
		String key=model.getClass().getName();
		
		Class<?> clazz=MelpClass.findClassWithAnnotation(model.getClass(),DB.class);
		if(clazz==null){
			if(model.$db==null){
				throw new RuntimeException("Dynamic model can not found DB, call model.use(DB) first!");
			}
			
			Table table=model.getClass().getAnnotation(Table.class);
			if(table==null){
				if(model.$tableName==null || model.$tableName.trim().length()<1){
					throw new RuntimeException("Dynamic model can not found table: "+model.$tableName+", call model(TableName) first!");
				}else{				
					key="#"+model.$db.getKey()+"$"+model.$tableName;
				}
			}
		}else{
			Table table=model.getClass().getAnnotation(Table.class);
			if(table==null){
				if(model.$tableName==null || model.$tableName.trim().length()<1){
					throw new RuntimeException("Dynamic model can not found table: "+model.$tableName+", call model(TableName) first!");
				}else{	
					DBConfig db=Model.dsm.getDBConfig(clazz);
					key=db.getKey()+"$"+model.$tableName;
				}
			}
		}
	 
		return key;
	}
	
	protected DBConfig  db;
	protected Dialect   dialect;
	
	protected String      tableName   = null;
	protected String[]    primaryKeys = null;
	protected Validator   validator   = null;
	 
	protected FGS       autoField;	
	protected Table     table;
	
	protected MetaPartition    mp;		 
	protected ModelListener    listener;
	protected List<ModelIndex> indexes=new ArrayList<ModelIndex>();
	
	protected Map<String,FGS> hFieldsByColumnName=new LinkedHashMap<String, MelpClass.FGS>();
	protected Map<String,FGS> hFieldsByJavaName  =new LinkedHashMap<String, MelpClass.FGS>();
	
	protected boolean record=false;
	protected boolean changed=false;
	
	protected String key;
	private ModelMeta(String key){		
		this.key=key;
	}
	
	void init(Model<?> model){		
		this.tableName  =model.$tableName;
		this.primaryKeys=model.$primaryKeys;
		
		initDB(model);
		
		initTable(model);
				
		initFields(model);
		
		initIndexes(model);
		
		initListeners(model);
		
		initPartioners(model); 
	}
	
	protected void initDB(Model<?> model) {
		Class<?> clazz=MelpClass.findClassWithAnnotation(model.getClass(),DB.class);
		if(clazz!=null){
			db=Model.dsm.getDBConfig(clazz);
		}else{
			db=model.$db;
		}
		
		if(db==null){
			throw new RuntimeException("Model: "+model.getClass()+" must implement interface annotated by: "+DB.class+", Or call use(db) first!");
		}
		
		dialect=Model.dsm.getDialect(db);
	}
	
	protected void initTable(Model<?> model) {
		table=model.getClass().getAnnotation(Table.class);			  
		if(table==null){
			if(tableName==null || tableName.trim().length()==0){
				tableName=model.getClass().getSimpleName();
			}
			
			table=createTable(tableName,primaryKeys);
		}else{
			tableName=table.name();
			primaryKeys=table.primaryKeys();
		}
	}
	
	protected void initFields(Model<?> model){
		List<FGS> fields=loadModelFields(model);		
		
		List<String> pks=new ArrayList<String>();
		for(Object o:fields){
			FGS fgs=(FGS)o;				
			Column c=fgs.getAnnotation(Column.class);
			
			hFieldsByColumnName.put(c.name().toLowerCase(),fgs);
			hFieldsByJavaName  .put(fgs.getFieldName().toLowerCase(),fgs);
			
			if(c.auto() && autoField==null){
				autoField=fgs;					 
			}
			
			if(c.key()){
				pks.add(c.name());
			}
		}
		
		if(primaryKeys==null || primaryKeys.length==0){
			primaryKeys=pks.toArray(new String[0]);
		}				
	}
	
	protected void initIndexes(Model<?> model) {
		Index[] tbIndexes=table.indexes();
		
		boolean primaryKey=false;
		if(tbIndexes!=null && tbIndexes.length>0){
			for(Index index:tbIndexes){ 
				ModelIndex mIndex=createModelIndexByIndex(index);
				
				if(mIndex.isPrimary()){
					primaryKey=true;
					indexes.add(0, mIndex);
				}else{
					indexes.add(mIndex);
				}
			}
		}
		
		String[] pks=table.primaryKeys();
		if(!primaryKey && pks!=null && pks.length>0){
			indexes.add(0, createModelIndexByPrimary(pks));
		}
	}
	
	protected ModelIndex createModelIndexByIndex(Index index) {
		ModelIndex mIndex=new ModelIndex();
		mIndex.setName(index.name());
		mIndex.setType(index.type());
		mIndex.setUnique(index.unique());
		
		List<FGS> fs=new ArrayList<MelpClass.FGS>();
		for(String f:index.fields()){
			FGS x=findFieldByName(f);
			
			assert x!=null;
			
			fs.add(x);
		}
		mIndex.setFields(fs);
		mIndex.setPrimary(isPrimary(fs));
		
		return mIndex;
	}
	
	protected ModelIndex createModelIndexByPrimary(String[] pks){
		ModelIndex mIndex=new ModelIndex();
		mIndex.setName("__primaryIndex__");
		mIndex.setType(0);
		mIndex.setUnique(true);
		
		List<FGS> fs=new ArrayList<MelpClass.FGS>();
		for(String f:pks){
			FGS x=findFieldByName(f);
			
			assert x!=null;
			
			fs.add(x);
		}
		mIndex.setFields(fs);
		mIndex.setPrimary(true);
		
		return mIndex;
	}
	
	protected boolean isPrimary(List<FGS> fs){
		String s1 = field2String(fs);
		String s2 = field2String(getPkFields());
		
		return s2.length()>0 && s2.equals(s1);
	}
	
	private String field2String(List<FGS> fs){
		StringBuilder sb=new StringBuilder();
		for(FGS f:fs){
			if(sb.length()>0){
				sb.append(",");
			}
			sb.append(f.getFieldName());
		}
		return sb.toString();
	}
	
	protected void initListeners(Model<?> model){
		String ls=DbProp.PROP_TABLE_MODEL_LISTENER.getValue(db,tableName);
		
		if(ls==null){
			ls=db.getCfg().getModelListener();
		}
		
		if(ls!=null && ls.trim().length()>0){
			try{
				listener=(ModelListener)MelpClass.forName(ls.trim()).newInstance();
			}catch(Exception e){
				throw new RuntimeException("Invalid model listener class: "+ls.trim()+", "+e,e);
			}
		}		
	}
	
	protected void initPartioners(Model<?> model){
		mp=db.getCfg().getPartition(table.name());
	}
	
	protected List<FGS> loadModelFields(Model<?> model){
		ClassHelper metaClass=MelpClass.getClassHelper(model.getClass());
		List<FGS> fields=metaClass.getFieldsWithAnnotation(Column.class);						
		if(fields.isEmpty()){
			record=true;
			fields=loadFieldsFromDB(metaClass);	
		}
		
		return fields;		
	}
	
	public boolean checkChanged(){
		if(mTable!=null && changed==false){
			try{
				MetaTable t2=TableHelper.getMetaTable(db, tableName);
				if(isTableFieldChanged(this.mTable,t2)){
					logger.info("Table struct changed: "+tableName);
					this.changed=true;
					
					return true;
				}
			}catch(Exception e){
				logger.error("Check table: "+tableName+" exception: "+e,e);
			}
		}
		
		return false;
	}
	
	private boolean isTableFieldChanged(MetaTable t1,MetaTable t2){
		if(t1==null || t2==null){
			return false;
		}
		
		for(MetaColumn x:t1.getColumns()){
			MetaColumn y=t2.getColumn(x.getName());
			if(y!=null){
				if(x.getJdbcType()!=y.getJdbcType() || x.getLength() !=y.getLength()){
					return true;
				}
			}else{
				return true;
			}
		}
		
		for(MetaColumn x:t2.getColumns()){
			MetaColumn y=t1.getColumn(x.getName());
			if(y!=null){
				if(x.getJdbcType()!=y.getJdbcType() || x.getLength() !=y.getLength()){
					return true;
				}
			}else{
				return true;
			}
		}
		 
		String[] s1=t1.getSeqMapping();
		String[] s2=t2.getSeqMapping();
		if(s1==null && s2==null) {
			return false;
		}else if(s1==null && s2!=null) {
			return true;
		}else if(s1!=null && s2==null) {
			return true;
		}else if(!MelpString.join(s1,"&").equalsIgnoreCase(MelpString.join(s2,"&"))) {
			return true;
		}
		
		return false;
	}
	
	
	private MetaTable mTable;
	protected List<FGS> loadFieldsFromDB(ClassHelper metaClass) {
		try{
			mTable=TableHelper.getMetaTable(db, tableName);
			if(mTable!=null){				 
				List<FGS> fs=new ArrayList<FGS>();
				 
				for(MetaColumn c:mTable.getColumns()){
					FGS mfd=metaClass.getField(c.getJavaName());
					if(mfd==null){
						mfd=metaClass.getField(c.getName());
					}
					
					FGS fgs=createFGS(c,mfd);	
					fs.add(fgs);								 
				}		
				 
				this.table = createTable(tableName,mTable);
				
				logMetaTable(mTable,fs);
				
				return fs;
			}else{
				throw new RuntimeException("Table not found: "+tableName+", DB: "+db.getKey());
			}			
		}catch(SQLException e){
			throw new RuntimeException(e);
		}
	}
	
	private void logMetaTable(MetaTable mTable,List<FGS> fs) {
		StringBuilder sb=new StringBuilder();
		for(FGS f:fs){
			if(sb.length()>0){
				sb.append(", ");
			}
			sb.append(f.getFieldName());
		}
		sb.insert(0,"Load table: "+tableName+" {");
		
		String[] seqMapping = mTable.getSeqMapping();
		if(seqMapping!=null && seqMapping.length > 2) {
			sb.append(" : "+seqMapping[0]+" -> " +seqMapping[1]+"."+seqMapping[2]);
		}
		sb.append("}");
		
		logger.info(sb.toString());
		
	}
	 
	public FGS findFieldByName(String theFieldName){
		if(theFieldName == null){
			return null;
		}
		
		String name=theFieldName.trim().toLowerCase();
		
		name=Dialect.getRealname(name);
		
		FGS fgs=hFieldsByColumnName.get(name);
		if(fgs==null){
			fgs=hFieldsByJavaName.get(name);
		}
		return fgs;
	}
 
	
	public Collection<FGS> fields(){
		return hFieldsByColumnName.values();
	}
		 
	public List<FGS> getPkFields(){
		List<FGS> pks=new ArrayList<FGS>();
		 
		for(Object o:fields()){
			FGS fgs=(FGS)o;
			Column c=fgs.getAnnotation(Column.class);
			if(c.key()){
				pks.add(fgs);
			}
		}
		return pks;
	}
	
	/**
	 * 复制对象数据
	 * @param model model to copy
	 * @return copy of the model
	 */
	public Model<?> copyModel(Model<?> model){
		try{
			Model<?> x=model.shallow();
			
			ModelHolder f=model.holder();
			ModelHolder t=x.holder();
			 
			t.updateKey  = model.holder().updateKey;			
			t.readonly   = model.holder().readonly;
			t.dirty      = model.holder().dirty;
			t.entity     = model.holder().entity;
			  
			t.fieldFilterExcludeMode=model.holder().fieldFilterExcludeMode;
			t.fieldFilterSets.addAll(model.holder().fieldFilterSets);
			if(f.hModelValues!=null){
				t.getModelValues().putAll(f.hModelValues);
			}
			
			for(FGS fgs:model.fields()){				
				Object value=fgs.getObject(model);
				fgs.setObject(x, value);
			}
 			 	
			return x;
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	  
	protected void doValidate(Model<?> model) {
		String validate=DbProp.PROP_TABLE_VALIDATE.getValue(db,tableName);
		
		if(validate.equalsIgnoreCase("true") || validate.equals("1")){			
			List<String> errors=validate(model);
			if(errors.size()>0){
				throw new RuntimeException(errors.toString());
			}
		}
	}
   
	
	public boolean isClearChangesAfterLoad() {
		String value = DbProp.PROP_TABLE_CLEAR_CHANGES_AFTER_LOAD.getValue(db, tableName, "false");
		return "true".equalsIgnoreCase(value);
	}
	
	/**
	 * 校验字段数据的是否合法.
	 * 
	 * @param model model for validate
	 * @return 不合法的字段列表{字段名: 错误信息}. 如果没有错误, 则为空列表.
	 */
	public List<String> validate(Model<?> model){
		if(validator==null){
			String clazz=DbProp.PROP_TABLE_VALIDATOR.getValue(db,tableName);
			
			if(MelpString.isEmpty(clazz)){
				validator=new Validator();
			}else{
				try{
					validator=(Validator)MelpClass.forName(clazz.trim()).newInstance();
				}catch(Exception e){
					throw new RuntimeException(e);
				}
			}
		}
		
		return validator.validate(model);
	}
	  
	
	public List<ModelIndex> uniqueIndexes(){
		List<ModelIndex> unique=new ArrayList<ModelIndex>();
		for(ModelIndex index:indexes){
			if(index.isUnique()){
				unique.add(index);
			}
		}		  
		return unique;
	}
	
	
	boolean isChanged(){
		return changed;
	}


	protected static FGS createFGS(final MetaColumn c, final FGS mfd) {
		return new FGS(mfd == null ? null : mfd.getType(), c.getJavaName(), c.getName()) {
			private Column columnAnnotation;

			@Override
			public void setObject(Object bean, Object v) {
				if (mfd != null) {
					mfd.setObject(bean, v);
				} else {
					setObjectForModel(bean, v);
				}
			}

			private void setObjectForModel(Object bean, Object v) {
				if (bean instanceof Model<?>) {
					Model<?> m = (Model<?>) bean;

					if (Date.class.getName().equals(c.getJavaType())) {
						v = MelpClass.converter.convert(v, Date.class);
					} else {
						try {
							String jtype = c.getJavaType();
							if (jtype.indexOf(".") < 0) {
								jtype = "java.lang." + jtype;
							}

							v = MelpClass.converter.convert(v, Class.forName(jtype));
						} catch (ClassNotFoundException e) {
							throw new RuntimeException("Convert: " + v + " to class exception: " + c.getJavaType(), e);
						}
					}

					m.holder().set(c.getName(), v);
				}
			}

			@Override
			public Object getObject(Object bean) {
				if (mfd != null) {
					return mfd.getObject(bean);
				} else {
					return getObjectForModel(bean);
				}
			}

			private Object getObjectForModel(Object bean) {
				if (bean instanceof Model<?>) {
					Model<?> m = (Model<?>) bean;
					return m.holder().get(c.getName());
				} else {
					return null;
				}
			}

			@Override
			public Field getField() {
				return mfd == null ? null : mfd.getField();
			}

			@Override
			@SuppressWarnings("unchecked")
			public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
				if (annotationClass == Column.class) {
					if (columnAnnotation == null) {
						columnAnnotation = createColumn(c);
					}
					return (T) columnAnnotation;
				} else {
					return null;
				}
			}
		};
	}


	protected static Column createColumn(final MetaColumn c){
		return new Column(){								 
			public Class<? extends Annotation> annotationType() {
				return Column.class;
			}
								 
			public String value() {							 
				return c.getValue();
			}
		 	 
			public String table() {							 
				return c.getTable().getName();
			}
		 	 
			public String remarks() {							 
				return c.getRemarks();
			}
									 
			public boolean notnull() {
				 
				return c.isNotnull();
			}
						 
			public String name() {
				return c.getName();
			}
			 
			public int length() {				 
				return c.getLength();
			}
			  
			public boolean key() {
				return c.isKey();
			}
			 
			public int jdbcType() {				 
				return c.getJdbcType();
			}
					 
			public boolean auto() {				 
				return c.isAuto();
			}
			
			public String seq() {				 
				return c.getSeq();
			}
			
			public int decimalDigits(){
				return c.getDecimalDigits();
			}
		};
	}
	
	
	public static Table createTable(final String tableName,final MetaTable metaTable){
		return new Table(){  
			public String name() {					 
				return tableName;
			}
			
			public String value() {					 
				return tableName;
			}
	 			
			public String remarks() {
				return metaTable.getRemarks();
			}
			
			public String[] primaryKeys(){
				List<String> pks=new ArrayList<String>();
				for(MetaColumn c:metaTable.getKeyColumns()){
					pks.add(c.getName());
				}
				
				return pks.toArray(new String[0]);
			}
			
			public Index[] indexes(){
				List<Index> indexes=new ArrayList<Index>();
				for(MetaIndex index:metaTable.getIndexes()){
					indexes.add(index.toIndexAnnotation());
				}
				
				return indexes.toArray(new Index[0]);
			}
			
			public Class<? extends Annotation> annotationType() {
				return Table.class;
			}
		};
	}
	
	public static Table createTable(final String tableName,final Table modelTable){
		return new Table(){  
			public String name() {					 
				return tableName;
			}
			
			public String value() {					 
				return tableName;
			}
	 			
			public String remarks() {
				return modelTable.remarks();
			}
			
			public String[] primaryKeys(){
				return modelTable.primaryKeys();
			}
			
			public Index[] indexes(){
				return modelTable.indexes();
			}
			
			public Class<? extends Annotation> annotationType() {
				return Table.class;
			}
		};
	}
	
	public static Table createTable(final String tableName,final String ...primaryKeys){
		return new Table(){ 
			public Class<? extends Annotation> annotationType() {
				return Table.class;
			}
			 
			public String value() {				 
				return tableName;
			}

			 
			public String name() {				 
				return tableName;
			}
			 
			public String remarks() {				 
				return "";
			}
 
			public String[] primaryKeys() {				 
				return primaryKeys;
			}
			 
			public Index[] indexes() {				 
				return new Index[0];
			}			
		};
	}
}