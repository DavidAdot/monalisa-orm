package test.com.tsc9526.monalisa.core.query;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.tsc9526.monalisa.core.annotation.DB;
import com.tsc9526.monalisa.core.datasource.DBConfig;
import com.tsc9526.monalisa.core.query.model.Model;
import com.tsc9526.monalisa.core.query.model.SimpleModel;
import com.tsc9526.monalisa.core.tools.ClassHelper.FGS;

@DB(url="jdbc:mysql://127.0.0.1:3306/jy_market", username="jy_market", password="jy_market")
public class SimpleModelTest extends Model<SimpleModelTest>{ 
	private static final long serialVersionUID = -1974865252589672370L;

	String title;
	
	public SimpleModelTest() {
		 super("gift");
	}
	
	
	public static void main(String[] args) {
		Object ix=0;
		
		System.out.println(ix instanceof Integer);
		
		DBConfig db=DBConfig.fromClass(SimpleModelTest.class);
		SimpleModel tx=new SimpleModel("gift");
		tx.use(db);
		
		tx.set("gift_id", 1);
		tx.load();
		System.out.println("=="+tx);
		
		SimpleModel tx2=new SimpleModel("gift_code");
		tx2.use(db);
		for(FGS fgs:tx2.fields()){
			System.out.println("=="+fgs.getFieldName());
		}
		if(tx2.field("package_name")!=null){
			throw new RuntimeException("Error dynamic model!");
		}
		
		
		
		Map<String, Object> hs=new LinkedHashMap<String, Object>();
		hs.put("x",new SimpleModelTest());
		hs.put("y",new SimpleModelTest());
		Object c=hs.values();
		System.out.println(c);
		Object c2=hs.values();
		System.out.println(c2);
	 	
		SimpleModelTest m=new SimpleModelTest();
		m.set("package_name", "package_name");
		
		m.title="xxx";
		
		m.set("icon_path", "icon_path");
		m.set("description", "description");
		m.set("begin_time", new Date());
		m.set("end_time", new Date());
		m.save();
		
		System.out.println("Create gift: "+m.get("gift_id"));
		
		SimpleModelTest t=new SimpleModelTest();
		t.set("gift_id", m.get("gift_id"));
		t.load();
		System.out.println(t);
		
		
		System.out.println("NEW: "+new SimpleModelTest());
		
		int times=10000;
		long l1=System.currentTimeMillis();
		for(int i=0;i<times;i++){
			SimpleModelTest x=new SimpleModelTest();
			x.changedFields();
		}
		long l2=System.currentTimeMillis();
		System.out.println("Use time: "+(l2-l1)+" ms");
		
		
		SimpleModel sm=new SimpleModel("gift");
		sm.use(db);
		for(SimpleModel x:sm.WHERE().field("package_name").like("package_name").field("giftId").gt(10).forSelect().select()){
			System.err.println(x.toString());
		}
	}

}