package cn.jcl.springbootquartzdemo;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * MyBatis代码生产器
 */
public class MybatisCodeGenerator {
    private static boolean genSwaggerAnnotations = true; //生成swagger注解
    private static String table = "message"; //目标表名
    private static String objectName = "Message"; //对象名称
    private static String ip = "192.168.2.83";//数据库ip
    private static String port = "3306";//端口
    private static String dbName = "icss";//数据库实例名
    private static String user = "root";//账号
    private static String psd = "123456";//密码
    private static String companyNameLowerCase = "centerm";//公司名称缩写(小写)
    private static String project_url = System.getProperty("user.dir") + "\\src\\main\\java\\";//源码路径
    private static String projectNameLowerCase = getProjectName().toLowerCase();//项目名称(小写)
    //    private static String mbt_xml_package = "com\\" + companyNameLowerCase + "\\" + projectNameLowerCase + "\\mapper\\xml"; //*.mapper.xml路径
    private static String mbt_xml_package = System.getProperty("user.dir") + "\\src\\main\\resources\\mybatis\\";//xml路径
    private static String bean_package = "com\\" + companyNameLowerCase + "\\" + projectNameLowerCase + "\\vo"; //Vo类路径
    private static String dao_package = "com\\" + companyNameLowerCase + "\\" + projectNameLowerCase + "\\mapper"; //Mapper路径
    private static String service_package = "com\\" + companyNameLowerCase + "\\" + projectNameLowerCase + "\\service"; //Service路径
    private static String encode = "UTF-8";
    private static String url = "jdbc:mysql://" + ip + ":" + port + "/" + dbName;
    private static String driver = "com.mysql.jdbc.Driver";
    private static String sql = "select * from " + table + " limit 0,1";
    private static String commentSQL = "show full fields from " + table;
    private static String getTableComment = "select * from INFORMATION_SCHEMA.TABLES where  TABLE_SCHEMA = '" + dbName + "' and TABLE_NAME='" + table + "'";
    private static String keySQL = "select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA = '" + dbName + "' and table_name='" + table
            + "' AND COLUMN_KEY='PRI'";
    private static List<String> keyList = getKeyList(); // 主键列表,下面多次用到
    private static String tableComment = getTableComment();


    private static Map<String, String> map; // 数据库对应的bean类型

    static {

        map = new HashMap<String, String>();
        map.put("VARCHAR", "String");
        map.put("varchar", "String");
        map.put("INT", "Integer");
        map.put("int", "Integer");
        map.put("INTEGER", "Integer");
        map.put("FLOAT", "Float");
        map.put("DOUBLE", "Double");
        map.put("DATE", "String");
        map.put("TIMESTAMP", "String");
        map.put("CHAR", "String");
        map.put("DATETIME", "String");
        map.put("LIST_IMPORT", "import java.util.List;");
        map.put("MAP_IMPORT", "import java.util.Map;");
        map.put("longtext", "String");
        map.put("text", "String");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("------代码生成开始------");
        System.out.println("源码路径：" + project_url);
        System.out.println("主键：" + keyList);
        geneMbtXml();
        geneBean();
        geneDAO();
        geneService();
        System.out.println("------代码生成结束------");
    }

    public static String getProjectName() {
        String path = System.getProperty("user.dir");
        int index = path.lastIndexOf("\\");
        return path.substring(index + 1).replaceAll("-", "");
    }

    /**
     * 生成Mbt xml文件
     */
    public static void geneMbtXml() throws Exception {
        System.out.println("生成xml");
//        File file = new File(project_url + mbt_xml_package + "/" + objectName + "Mapper.xml");
        File file = new File(mbt_xml_package + "/" + objectName + "Mapper.xml");
        Document document = DocumentHelper.createDocument();
        document.addDocType("mapper", "-//mybatis.org//DTD Mapper 3.0//EN", "http://mybatis.org/dtd/mybatis-3-mapper.dtd");
        document.addComment(table + "(" + tableComment + ")");
        Element rootEle = document.addElement("mapper");
        rootEle.addAttribute("namespace", getDAOPackage() + "." + objectName + "Mapper");
//		addResultMapElement(rootEle); // 添加 resultMap
        rootEle.addText("\r\n\t");
        addSelectElement(rootEle);
        rootEle.addText("\r\n\t");
        addSelectCountElement(rootEle);
        rootEle.addText("\r\n\t");
        addInsertElement(rootEle);
        if (keyList != null && keyList.size() != 0) {
            rootEle.addText("\r\n\t");
            addUpdateElement(rootEle);
            rootEle.addText("\r\n\t");
//            addDeleteElement(rootEle);
        }
        writeDocToFile(document, file);
    }

    /**
     * 生成 bean 文件
     */
    public static void geneBean() {
        System.out.println("生成vo");
        File file = new File(project_url + bean_package + "/" + objectName + "Vo.java");
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }

        StringBuffer bean = new StringBuffer("package " + getBeanPackage() + ";" + "\r\r");
        if (genSwaggerAnnotations) {
            bean.append("import io.swagger.annotations.ApiModel;");
            bean.append("import io.swagger.annotations.ApiModelProperty;\r\r");
        }

        try {
            ResultSetMetaData rsmd = getResultSetMetaData(sql);
            Map<String, String> commentMap = getColumnComments();
            // 需要导入的包
            for (int j = 1; j <= rsmd.getColumnCount(); j++) {
                String columnDbType = rsmd.getColumnTypeName(j); // 数据库类型
                String im = getImport(columnDbType + "_IMPORT");
                if (im != null) {
                    bean.append(im).append("\r\n\r\n");
                }
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/dd");
            bean.append("/**\r\n * " + tableComment + "Vo类\r\n * Created by " + MybatisCodeGenerator.class.getSimpleName() + " on " + sdf.format(new Date()) + "\r\n */\r\n");
            if (genSwaggerAnnotations) bean.append("@ApiModel(\"" + tableComment + "\")\r");
            bean.append("public class " + objectName + "Vo{\r");

            // 生成的属性
            for (int j = 1; j <= rsmd.getColumnCount(); j++) {
                String columnDbType = rsmd.getColumnTypeName(j); // 数据库类型
                String columnName = rsmd.getColumnName(j); // 字段名
                String column = "";
                if (genSwaggerAnnotations) {
                    column = "\t@ApiModelProperty(\"" + commentMap.get(rsmd.getColumnName(j)) + "\")\r";
                    column += "\tprivate " + getPojoType(columnDbType) + " " + columnToPropertyName(columnName)
                            + ";";
                } else {
                    column += "\tprivate " + getPojoType(columnDbType) + " " + columnToPropertyName(columnName)
                            + ";//" + commentMap.get(rsmd.getColumnName(j));
                }
                bean.append(column).append("\r\n\r\n");
            }

            // 生成的get/set
            for (int j = 1; j <= rsmd.getColumnCount(); j++) {
                String columnDbType = rsmd.getColumnTypeName(j); // 数据库类型
                String columnName = rsmd.getColumnName(j); // 字段名
                bean.append(getMethodStr(columnToPropertyName(columnName), getPojoType(columnDbType))).append("\r\n");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        bean.append("}");

        writeToFile(file, bean.toString());
    }

    /**
     * 生成 service 文件内容
     */
    public static void geneService() {
        System.out.println("生成Service");
        File file = new File(project_url + service_package + "/I" + objectName + "Service.java");
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }

        StringBuffer dao = new StringBuffer("package " + getServicePackage() + ";" + "\r\r");
        // mapper.append("import org.springframework.stereotype.Repository;").append("\r");
        dao.append(getPojoType("LIST_IMPORT")).append("\r");
        // mapper.append(getPojoType("MAP_IMPORT")).append("\r\r");
        dao.append("import " + getBeanPackage() + "." + objectName + "Vo;").append("\r\r\n"); // 导入bean类
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/dd");
        dao.append("/**\r\n * " + tableComment + "Service接口\r\n * Created by " + MybatisCodeGenerator.class.getSimpleName() + " on " + sdf.format(new Date()) + "\r\n */\r\n");
        dao.append("public interface I" + objectName + "Service{\r\r\n");

        dao.append("\t").append("/**").append("\r\t*获取列表\r").append("\t*/").append("\r");
        dao.append("\t")
                .append("List<" + objectName + "Vo> query" + objectName + "List(" + objectName + "Vo "
                        + columnToPropertyName(objectName) + ");").append("\r\r\n");
        dao.append("\t").append("/**").append("\r\t*获取总数\r").append("\t*/").append("\r");
        dao.append("\t")
                .append("int query" + objectName + "Count(" + objectName + "Vo "
                        + columnToPropertyName(objectName) + ");").append("\r\r\n");
        dao.append("\t").append("/**").append("\r\t*添加\r").append("\t*/").append("\r");
        dao.append("\t")
                .append("Integer insert" + objectName + "(" + objectName + "Vo "
                        + columnToPropertyName(objectName) + ");").append("\r\r\n");
        if (keyList.size() > 0) {
            dao.append("\t").append("/**").append("\r\t*修改\r").append("\t*/").append("\r");
            dao.append("\t")
                    .append("Integer update" + objectName + "(" + objectName + "Vo "
                            + columnToPropertyName(objectName) + ");").append("\r\r\n");
//            mapper.append("\t").append("/**").append("\r\t*删除\r").append("\t*/").append("\r");
//            if (keyList.size() == 1) {
//                mapper.append("\t")
//                        .append("Integer delete" + objectName + "By"
//                                + upperFirestChar(columnToPropertyName(keyList.get(0))) + "("
//                                + getPojoType(getDataTypeByColumnName(keyList.get(0))) + " "
//                                + columnToPropertyName(keyList.get(0)) + ");").append("\r\n");
//            } else {
//                mapper.append("\t")
//                        .append("Integer delete" + objectName + "(" + objectName + " "
//                                + columnToPropertyName(objectName) + ");").append("\r\n");
//            }

        }
        dao.append("}\r");
        writeToFile(file, dao.toString());

        geneServiceImpl(); // 实现类
    }

    /**
     * 生成 service 文件内容
     */
    public static void geneServiceImpl() {
        File file = new File(project_url + service_package + "/impl/" + objectName + "Service.java");
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }

        StringBuffer dao = new StringBuffer("package " + getServicePackage() + ".impl;" + "\r\r");
        // mapper.append("import org.springframework.stereotype.Repository;").append("\r");
        dao.append(getPojoType("LIST_IMPORT")).append("\r");
        // mapper.append(getPojoType("MAP_IMPORT")).append("\r\r");
        dao.append("import " + getServicePackage() + ".I" + objectName + "Service;").append("\r\n"); // 导入bean类
        dao.append("import " + getBeanPackage() + "." + objectName + "Vo;").append("\r\r\n"); // 导入bean类
        dao.append("import " + getDAOPackage() + "." + objectName + "Mapper;").append("\r\r\n"); // 导入bean类

        dao.append("import org.springframework.stereotype.Service;").append("\r"); // 导入bean类
        dao.append("import org.springframework.transaction.annotation.Transactional;").append("\r"); // 导入bean类
        dao.append("import javax.annotation.Resource;").append("\r\r"); // 导入bean类
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/dd");
        dao.append("/**\r\n * " + tableComment + "Service实现类\r\n * Created by " + MybatisCodeGenerator.class.getSimpleName() + " on " + sdf.format(new Date()) + "\r\n */\r\n");
        dao.append("@Service").append("\r\n");
        dao.append("@Transactional").append("\r\n");
        dao.append("public class " + objectName + "Service implements I" + objectName + "Service{\r\r\n");
        dao.append("\t").append("@Resource").append("\r\n");
        dao.append("\t").append("private " + objectName + "Mapper " + columnToPropertyName(objectName) + "Mapper;")
                .append("\r\r\n");

        // ------------------------------列表start---------------------------------
        dao.append("\t").append("/**").append("\r\t*获取列表\r").append("\t*/").append("\r");
        dao.append("\t")
                .append("public List<" + objectName + "Vo> query" + objectName + "List(" + objectName + "Vo "
                        + columnToPropertyName(objectName) + "){\r\t\t return " + columnToPropertyName(objectName)
                        + "Mapper.query" + objectName + "List(" + columnToPropertyName(objectName) + ");\r\t}")
                .append("\r\r\n");
        // ------------------------------列表end---------------------------------
        // ------------------------------总数start---------------------------------
        dao.append("\t").append("/**").append("\r\t*获取总数\r").append("\t*/").append("\r");
        dao.append("\t")
                .append("public int query" + objectName + "Count(" + objectName + "Vo "
                        + columnToPropertyName(objectName) + "){\r\t\t return " + columnToPropertyName(objectName)
                        + "Mapper.query" + objectName + "Count(" + columnToPropertyName(objectName) + ");\r\t}")
                .append("\r\r\n");
        // ------------------------------总数end---------------------------------

        // ------------------------------添加start---------------------------------
        dao.append("\t").append("/**").append("\r\t*添加\r").append("\t*/").append("\r");
        dao.append("\t")
                .append("public Integer insert" + objectName + "(" + objectName + "Vo "
                        + columnToPropertyName(objectName) + "){\r\t\t return " + columnToPropertyName(objectName)
                        + "Mapper.insert" + objectName + "(" + columnToPropertyName(objectName) + ");\r\t}")
                .append("\r\r\n");
        // ------------------------------添加end---------------------------------

        if (keyList.size() > 0) {
            // ------------------------------修改start---------------------------------
            dao.append("\t").append("/**").append("\r\t*修改\r").append("\t*/").append("\r");
            dao.append("\t")
                    .append("public Integer update" + objectName + "(" + objectName + "Vo "
                            + columnToPropertyName(objectName) + "){\r\t\t return " + columnToPropertyName(objectName)
                            + "Mapper.update" + objectName + "(" + columnToPropertyName(objectName) + ");\r\t}")
                    .append("\r\r\n");
            // ------------------------------修改end---------------------------------

//            mapper.append("\t").append("/**").append("\r\t*删除\r").append("\t*/").append("\r");
//            if (keyList.size() == 1) {
//                mapper.append("\t")
//                        .append("public Integer delete" + objectName + "By"
//                                + upperFirestChar(columnToPropertyName(keyList.get(0))) + "("
//                                + getPojoType(getDataTypeByColumnName(keyList.get(0))) + " "
//                                + columnToPropertyName(keyList.get(0)) + "){\r\t\t return "
//                                + columnToPropertyName(objectName) + "Mapper.delete" + objectName + "By"
//                                + upperFirestChar(columnToPropertyName(keyList.get(0))) + "("
//                                + columnToPropertyName(keyList.get(0)) + ");\r\t};").append("\r\n");
//            } else {
//                mapper.append("\t")
//                        .append("public Integer delete" + objectName + "(" + objectName + " "
//                                + columnToPropertyName(objectName) + "){};").append("\r\n");
//            }

        }
        dao.append("}\r");
        writeToFile(file, dao.toString());
    }

    /**
     * 生成 mapper 文件内容
     */
    public static void geneDAO() {
        System.out.println("生成Mapper");
        File file = new File(project_url + dao_package + "/" + objectName + "Mapper.java");
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }

        StringBuffer dao = new StringBuffer("package " + getDAOPackage() + ";" + "\r\r");
        // mapper.append("import org.springframework.stereotype.Repository;").append("\r");
        dao.append(getPojoType("LIST_IMPORT")).append("\r");
        // mapper.append(getPojoType("MAP_IMPORT")).append("\r\r");
        dao.append("import " + getBeanPackage() + "." + objectName + "Vo;").append("\r\r\n"); // 导入bean类
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/dd");
        dao.append("/**\r\n * " + tableComment + "Mapper接口\r\n * Created by " + MybatisCodeGenerator.class.getSimpleName() + " on " + sdf.format(new Date()) + "\r\n */\r\n");
        dao.append("public interface " + objectName + "Mapper{\r\r\n");

        dao.append("\t").append("/**").append("\r\t*获取列表\r").append("\t*/").append("\r");
        dao.append("\t")
                .append("List<" + objectName + "Vo> query" + objectName + "List(" + objectName + "Vo "
                        + columnToPropertyName(objectName) + ");").append("\r\r\n");
        dao.append("\t").append("/**").append("\r\t*获取总数\r").append("\t*/").append("\r");
        dao.append("\t")
                .append("int query" + objectName + "Count(" + objectName + "Vo "
                        + columnToPropertyName(objectName) + ");").append("\r\r\n");
        dao.append("\t").append("/**").append("\r\t*添加\r").append("\t*/").append("\r");
        dao.append("\t")
                .append("Integer insert" + objectName + "(" + objectName + "Vo "
                        + columnToPropertyName(objectName) + ");").append("\r\r\n");
        if (keyList.size() > 0) {
            dao.append("\t").append("/**").append("\r\t*修改\r").append("\t*/").append("\r");
            dao.append("\t")
                    .append("Integer update" + objectName + "(" + objectName + "Vo "
                            + columnToPropertyName(objectName) + ");").append("\r\r\n");
//            mapper.append("\t").append("/**").append("\r\t*删除\r").append("\t*/").append("\r");
//            if (keyList.size() == 1) {
//                mapper.append("\t")
//                        .append("Integer delete" + objectName + "By"
//                                + upperFirestChar(columnToPropertyName(keyList.get(0))) + "("
//                                + getPojoType(getDataTypeByColumnName(keyList.get(0))) + " "
//                                + columnToPropertyName(keyList.get(0)) + ");").append("\r\n");
//            } else {
//                mapper.append("\t")
//                        .append("Integer delete" + objectName + "(" + objectName + " "
//                                + columnToPropertyName(objectName) + ");").append("\r\n");
//            }

        }
        dao.append("}\r");
        writeToFile(file, dao.toString());
    }

    /**
     * 生成mbt xml文件内容
     */
    public static void addResultMapElement(Element rootEle) {
        try {

            Element resultMap = rootEle.addElement("resultMap");

            resultMap.addAttribute("type", getBeanPackage() + "." + objectName + "Vo");
            resultMap.addAttribute("id", columnToPropertyName(objectName) + "_resultMap");

            ResultSetMetaData rsmd = getResultSetMetaData(sql);
            for (int j = 1; j <= rsmd.getColumnCount(); j++) {
                String columnName = rsmd.getColumnName(j); // 字段名
                Element result = resultMap.addElement("result");
                result.addAttribute("property", columnToPropertyName(columnName));
                result.addAttribute("column", columnName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成mbt select内容
     */
    public static void addSelectElement(Element rootEle) {
        rootEle.addComment("获取列表");
        Element resultMap = rootEle.addElement("select");
        resultMap.addAttribute("id", "query" + objectName + "List");
        resultMap.addAttribute("parameterType", getBeanPackage() + "." + objectName + "Vo");
//		resultMap.addAttribute("resultMap", columnToPropertyName(objectName) + "_resultMap");
        resultMap.addAttribute("resultType", getBeanPackage() + "." + objectName + "Vo");

        String colnums = "";
        ResultSetMetaData rsmd = getResultSetMetaData(sql);
        try {
            for (int j = 1; j <= rsmd.getColumnCount(); j++) {
                String columnName = rsmd.getColumnName(j); // 字段名
                colnums = colnums + columnName + ",";
            }
            colnums = colnums.substring(0, colnums.length() - 1);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        resultMap.addText("\r\t\t").addText("SELECT " + colnums + " FROM " + table).addText("\r\n\t");
    }

    /**
     * 生成mbt select Count内容
     */
    public static void addSelectCountElement(Element rootEle) {
        rootEle.addComment("获取总数");
        Element resultMap = rootEle.addElement("select");
        resultMap.addAttribute("id", "query" + objectName + "Count");
        resultMap.addAttribute("parameterType", getBeanPackage() + "." + objectName + "Vo");
        resultMap.addAttribute("resultType", "java.lang.Integer");
        resultMap.addText("\r\t\t").addText("SELECT COUNT(1) FROM " + table).addText("\r\n\t");
    }

    /**
     * 生成mbt insert内容
     */
    public static void addInsertElement(Element rootEle) {
        rootEle.addComment("添加");
        Element resultMap = rootEle.addElement("insert");
        resultMap.addAttribute("id", "insert" + objectName);
        resultMap.addAttribute("parameterType", getBeanPackage() + "." + objectName + "Vo").addText("\r\t");
        if (keyList.size() > 0) {
            resultMap.addAttribute("useGeneratedKeys", "true");
            resultMap.addAttribute("keyProperty", keyList.get(0));
        }
        resultMap.addText(genMybatisInsertSQL());
    }

    /**
     * 生成mbt update内容
     */
    public static void addUpdateElement(Element rootEle) {
        rootEle.addComment("修改");
        Element resultMap = rootEle.addElement("update");
        resultMap.addAttribute("id", "update" + objectName);
        resultMap.addAttribute("parameterType", getBeanPackage() + "." + objectName + "Vo");
        resultMap.addText("\r\t\tUPDATE " + table);
        addUpdateSetElement(resultMap);
        resultMap.addText("\r").addText("\t\tWHERE ");
        StringBuffer updateById = new StringBuffer();
        for (String string : keyList) {
            updateById.append(string).append(" = #{" + columnToPropertyName(string) + "} and ");
        }
        updateById.delete(updateById.length() - 4, updateById.length());
        resultMap.addText(updateById.toString());

    }

    /**
     * 添加更新的set
     */
    public static void addUpdateSetElement(Element rootEle) {
        try {
            Element set = rootEle.addElement("set");

            ResultSetMetaData rsmd = getResultSetMetaData(sql);
            for (int j = 1; j <= rsmd.getColumnCount(); j++) {
                String columnName = rsmd.getColumnName(j); // 字段名
                if (!isKey(columnName)) {
                    Element ifElement = set.addElement("if");
                    ifElement.addAttribute("test", columnToPropertyName(columnName) + " != null and " + columnToPropertyName(columnName) + " != '' ");
                    ifElement.addText(columnName + "=#{" + columnToPropertyName(columnName) + "}").addText(",");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成mbt delete内容
     */
    public static void addDeleteElement(Element rootEle) {

        Element resultMap = rootEle.addElement("delete");
        if (keyList.size() == 1) {
            resultMap.addAttribute("id",
                    "delete" + objectName + "By" + upperFirestChar(columnToPropertyName(keyList.get(0))));
            resultMap.addText("\r\t\t")
                    .addText("DELETE FROM " + table + " WHERE " + keyList.get(0) + " = #{" + keyList.get(0) + "}")
                    .addText("\r\t");
        } else {
            resultMap.addAttribute("id", "delete" + objectName);
            resultMap.addAttribute("parameterType", upperFirestChar(objectName));
            StringBuffer delById = new StringBuffer();
            for (String string : keyList) {
                delById.append(string).append(" = #{" + columnToPropertyName(string) + "} and ");
            }
            delById.delete(delById.length() - 4, delById.length());
            resultMap.addText("\r\t\t").addText("DELETE FROM " + table + " WHERE " + delById).addText("\r\t");
        }

    }

    /**
     * 得到Bean的包名
     */
    public static String getBeanPackage() {
        return bean_package.replace("\\", ".");
    }

    /**
     * 得到DAO的包名
     */
    public static String getDAOPackage() {
        return dao_package.replace("\\", ".");
    }

    /**
     * 得到Service的包名
     */
    public static String getServicePackage() {
        return service_package.replace("\\", ".");
    }

    /*
     * 重写字段反向生成POJO类中的成员变量的命名规则 第一个字母大写
     */
    public static String columnToPropertyName(String column) {

        if (column != null) {
            String[] tmp = column.split("_");
            // 最终的表名
            String finalPropertyName = "";
            if (tmp.length == 1) {
                // 没有下划线
                finalPropertyName = tmp[0];
            } else {

                finalPropertyName = tmp[0];
                for (int i = 1; i < tmp.length; i++) {

                    if (tmp[i] == null || tmp[i].toString().length() == 0) {
                        continue;
                    }
                    String temp = tmp[i];
                    String tmpChar = temp.substring(0, 1);
                    String postString = temp.substring(1, temp.length());
                    temp = tmpChar + postString;
                    finalPropertyName = finalPropertyName + temp;
                }
            }
            return finalPropertyName;
        }
        return "";
    }

    /**
     * 把doc 写到相应的xml文件中
     */
    public static void writeDocToFile(Document doc, File file) {
        try {
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
//            OutputFormat format = OutputFormat.createCompactFormat();
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setTrimText(false);
            format.setEncoding(encode);
            format.setIndent(true); //设置是否缩进
            format.setIndent("\t"); //以四个空格方式实现缩进
            XMLWriter output = new XMLWriter(new FileWriter(file), format);
            output.write(doc);
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据传进来的数据类型来得到相应的java类型字符串
     */
    public static String getPojoType(String dataType) {
        StringTokenizer st = new StringTokenizer(dataType);
        String javaType = map.get(st.nextToken());
        if (javaType == null || javaType.length() == 0) {
            System.err.print("不支持的类型:" + javaType);
        }
        return javaType;
    }

    /**
     * 需要导入的包
     */
    public static String getImport(String dataType) {
        if (map.get(dataType) == null || "".equals(map.get(dataType))) {
            return null;
        } else {
            return map.get(dataType);
        }
    }

    /**
     * 获取TABLE的元数据
     */
    public static ResultSetMetaData getResultSetMetaData(String sql) {
        Connection conn = getConn();
        PreparedStatement psmt = getPreparedStatement(conn, sql);
        ResultSetMetaData rsmd = null;
        ResultSet rs;
        try {
            rs = psmt.executeQuery();
            rsmd = rs.getMetaData();
        } catch (SQLException e) {
            System.out.println("获取ResultSetMetaData失败...在" + MybatisCodeGenerator.class.getName()
                    + ".ResultSetMetaData()方法中");
            e.printStackTrace();
        }
        return rsmd;
    }

    /**
     * 根据表名查询是该表所有字段的注释
     */
    public static Map<String, String> getColumnComments() {
        Connection conn = getConn();
        PreparedStatement commentPsmt = getPreparedStatement(conn, commentSQL);
        Map<String, String> commentMap = new HashMap<String, String>();
        try {
            ResultSet commentRs = commentPsmt.executeQuery();
            while (commentRs.next()) {
                // commentMap.put(commentRs.getString("COLUMN_NAME"),
                // commentRs.getString("COMMENTS"));
                commentMap.put(commentRs.getString("field"), commentRs.getString("COMMENT"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return commentMap;
    }

    /**
     * 获取表注释
     */
    public static String getTableComment() {
        Connection conn = getConn();
        PreparedStatement commentPsmt = getPreparedStatement(conn, getTableComment);
        String comment = "";
        try {
            ResultSet commentRs = commentPsmt.executeQuery();
            while (commentRs.next()) {
                comment = commentRs.getString("TABLE_COMMENT");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return comment;
    }

    /**
     * @param type
     * @return
     */
    private static String getMethodStr(String field, String type) {
        StringBuilder get = new StringBuilder("\tpublic ");
        get.append(type).append(" ");
        if (type.equals("boolean")) {
            get.append(field);
        } else {
            get.append("get");
            get.append(upperFirestChar(field));
        }
        get.append("(){").append("\r\n\t\treturn this.").append(field).append(";\r\n\t}\r\n");
        StringBuilder set = new StringBuilder("\tpublic void ");

        if (type.equals("boolean")) {
            set.append(field);
        } else {
            set.append("set");
            set.append(upperFirestChar(field));
        }
        set.append("(").append(type).append(" ").append(field).append("){\r\n\t\tthis.").append(field).append("=")
                .append(field).append(";\r\n\t}\r\n");
        get.append(set);
        return get.toString();
    }

    /**
     * 生成Mybatis的插入SQL
     */
    public static String genMybatisInsertSQL() {
        StringBuffer sb = new StringBuffer("\tINSERT INTO " + table + "(").append("\r");
        try {
            ResultSetMetaData rsmd = getResultSetMetaData(sql);
            for (int j = 1; j <= rsmd.getColumnCount(); j++) {
                if (keyList.size() > 0 && j == 1) continue;
                String columnName = rsmd.getColumnName(j); // 字段名
                sb.append("\t\t" + columnName);
                if (j != rsmd.getColumnCount()) {
                    sb.append(",").append("\r");
                }
            }
            sb.append("\r\t\t)VALUES(\r");
            for (int j = 1; j <= rsmd.getColumnCount(); j++) {
                if (keyList.size() > 0 && j == 1) continue;
                String columnName = rsmd.getColumnName(j); // 字段名
                sb.append("\t\t#{" + columnToPropertyName(columnName) + "}");
                if (j != rsmd.getColumnCount()) {
                    sb.append(",").append("\r");
                }
            }
            sb.append("\r\n\t\t)\r\n\t");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * 获取该表的主键
     */
    public static List<String> getKeyList() {
        List<String> keyList = new ArrayList<String>();
        Connection conn = getConn();
        PreparedStatement commentPsmt = getPreparedStatement(conn, keySQL);
        try {
            ResultSet commentRs = commentPsmt.executeQuery();
            while (commentRs.next()) {
                keyList.add(commentRs.getString("COLUMN_NAME"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return keyList;
    }

    /**
     * 是否主键
     */
    public static Boolean isKey(String columnName) {
        for (String string : keyList) {
            if (columnName.equals(string)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据字段获取该字段类型
     */
    public static String getDataTypeByColumnName(String columnName) {
        String sql = "select DATA_TYPE from INFORMATION_SCHEMA.COLUMNS where table_name='" + table
                + "' AND COLUMN_KEY='PRI' and column_name='" + columnName + "'";
        Connection conn = getConn();
        PreparedStatement commentPsmt = getPreparedStatement(conn, sql);
        try {
            ResultSet commentRs = commentPsmt.executeQuery();
            while (commentRs.next()) {
                return commentRs.getString("DATA_TYPE");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将字符的第一个字母改成大写
     *
     * @param src
     * @return
     */
    public static String upperFirestChar(String src) {
        return src.substring(0, 1).toUpperCase().concat(src.substring(1));
    }

    /**
     * 将内容写入到文件中去
     */
    public static void writeToFile(File file, String content) {
        try {
            OutputStreamWriter outputStream = new OutputStreamWriter(new FileOutputStream(file), encode);
            outputStream.write(content);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 通过JDBC方式获取数据库连接
     */
    public static Connection getConn() {
        Connection conn = null; // 连接
        try {
            Class.forName(driver); // 加载驱动
            conn = DriverManager.getConnection(url, user, psd); // 获取连接
        } catch (ClassNotFoundException e) {
            System.out.println("获取连接失败..........可能是出错原因:1.驱动包未加");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("获取连接失败...........可能是出错原因:1.驱动包未加,2.用户名密码出错,3.数据库连接URL或drive写错");
            e.printStackTrace();
        }
        return conn; // 返回连接
    }

    public static PreparedStatement getPreparedStatement(Connection con, String sql) {
        try {
            return con.prepareStatement(sql);
        } catch (SQLException e) {
            System.out.println("获取PreparedStatement失败!..................");
            e.printStackTrace();
        }
        return null;
    }


}
