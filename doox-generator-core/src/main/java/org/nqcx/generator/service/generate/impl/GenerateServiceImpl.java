/*
 * Copyright 2018 nqcx.org All right reserved. This software is the
 * confidential and proprietary information of nqcx.org ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with nqcx.org.
 */

package org.nqcx.generator.service.generate.impl;

import org.apache.commons.lang3.StringUtils;
import org.nqcx.doox.commons.lang.o.DTO;
import org.nqcx.generator.provide.o.CgField;
import org.nqcx.generator.provide.o.Generate;
import org.nqcx.generator.provide.o.table.Column;
import org.nqcx.generator.provide.o.table.Table;
import org.nqcx.generator.service.generate.GenerateService;
import org.nqcx.generator.service.table.TableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author naqichuan Feb 9, 2014 2:18:27 AM
 */
@Service
public class GenerateServiceImpl implements GenerateService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // database type to type and length
    private final static Pattern TYPE_LENGTH_PATTERN = Pattern.compile("(.+?)(\\(((\\d+?))\\))*?");
    // Class name mapping to class reference
    private final static Map<String, String> CLASS_MAPPING = new HashMap<>();

    private final static String JAVA_PATH = "src/main/java/";
    private final static String TEST_PATH = "src/test/java/";
    private final static String JAVA_EXT_NAME = ".java";
    private final static String XML_EXT_NAME = ".xml";

    private final static String O_TXT_TEMPLATE_NAME = "o.txt";
    private final static String PROVIDE_TXT_TEMPLATE_NAME = "provide.txt";
    private final static String PO_TXT_TEMPLATE_NAME = "po.txt";
    private final static String MAPPER_TXT_TEMPLATE_NAME = "mapper.txt";
    private final static String MAPPERXML_TXT_TEMPLATE_NAME = "mapperxml.txt";
    private final static String JPA_TXT_TEMPLATE_NAME = "jpa.txt";
    private final static String DAO_TXT_TEMPLATE_NAME = "dao.txt";
    private final static String DAO_TEST_TXT_TEMPLATE_NAME = "daotest.txt";
    private final static String DAOMAPPERIMPL_TXT_TEMPLATE_NAME = "daomapperimpl.txt";
    private final static String DAOJPAIMPL_TXT_TEMPLATE_NAME = "daojpaimpl.txt";
    private final static String DO_TXT_TEMPLATE_NAME = "do.txt";
    private final static String SERVICE_TXT_TEMPLATE_NAME = "service.txt";
    private final static String SERVICEIMPL_TXT_TEMPLATE_NAME = "serviceimpl.txt";
    private final static String SERVICETEST_TXT_TEMPLATE_NAME = "servicetest.txt";
    private final static String VO_TXT_TEMPLATE_NAME = "vo.txt";
    private final static String CONTROLLER_TXT_TEMPLATE_NAME = "controller.txt";

    @Autowired
    @Qualifier("overwrite")
    private Boolean overwrite; // 生成文件是否覆盖原来的文件
    @Autowired
    private TableService tableService;

    static {
        CLASS_MAPPING.put("Serializable", "java.io.Serializable");

        CLASS_MAPPING.put("Integer", "java.lang.Integer");
        CLASS_MAPPING.put("Long", "java.lang.Long");

        CLASS_MAPPING.put("Date", "java.util.Date");
        CLASS_MAPPING.put("Arrays", "java.util.Arrays");
        CLASS_MAPPING.put("ArrayList", "java.util.ArrayList");
        CLASS_MAPPING.put("List", "java.util.List");
        CLASS_MAPPING.put("Optional", "java.util.Optional");
        CLASS_MAPPING.put("Map", "java.util.Map");

        CLASS_MAPPING.put("Entity", "javax.persistence.Entity");
        CLASS_MAPPING.put("Table", "javax.persistence.Table");
        CLASS_MAPPING.put("ID", "javax.persistence.Id");
        CLASS_MAPPING.put("GenerationType", "javax.persistence.GenerationType");
        CLASS_MAPPING.put("Column", "javax.persistence.Column");
        CLASS_MAPPING.put("GeneratedValue", "javax.persistence.GeneratedValue");
        CLASS_MAPPING.put("Temporal", "javax.persistence.Temporal");

        CLASS_MAPPING.put("DTO", "org.nqcx.doox.commons.lang.o.DTO");
        CLASS_MAPPING.put("NPage", "org.nqcx.doox.commons.lang.o.NPage");
        CLASS_MAPPING.put("NSort", "org.nqcx.doox.commons.lang.o.NSort");

        CLASS_MAPPING.put("IMapper", "org.nqcx.doox.commons.data.mapper.IMapper");
        CLASS_MAPPING.put("MapperSupport", "org.nqcx.doox.commons.data.mapper.MapperSupport");
        CLASS_MAPPING.put("IJpa", "org.nqcx.doox.commons.data.jpa.IJpa");
        CLASS_MAPPING.put("JpaSupport", "org.nqcx.doox.commons.data.jpa.JpaSupport");
        CLASS_MAPPING.put("IDAO", "org.nqcx.doox.commons.dao.IDAO");

        CLASS_MAPPING.put("IService", "org.nqcx.doox.commons.service.IService");
        CLASS_MAPPING.put("ServiceSupport", "org.nqcx.doox.commons.service.ServiceSupport");

        CLASS_MAPPING.put("stereotype.Service", "org.springframework.stereotype.Service");
        CLASS_MAPPING.put("Autowired", "org.springframework.beans.factory.annotation.Autowired");
        CLASS_MAPPING.put("Qualifier", "org.springframework.beans.factory.annotation.Qualifier");

        CLASS_MAPPING.put("MediaType", "org.springframework.http.MediaType");
        CLASS_MAPPING.put("Controller", "org.springframework.stereotype.Controller");
        CLASS_MAPPING.put("PathVariable", "org.springframework.web.bind.annotation.PathVariable");
        CLASS_MAPPING.put("RequestMapping", "org.springframework.web.bind.annotation.RequestMapping");
        CLASS_MAPPING.put("RequestMethod", "org.springframework.web.bind.annotation.RequestMethod");
        CLASS_MAPPING.put("RequestParam", "org.springframework.web.bind.annotation.RequestParam");
        CLASS_MAPPING.put("ResponseBody", "org.springframework.web.bind.annotation.ResponseBody");
        CLASS_MAPPING.put("ModelAndView", "org.springframework.web.servlet.ModelAndView");

        CLASS_MAPPING.put("test.Test", "org.junit.Test");
    }

    /**
     * generate code main method
     *
     * @param g generate o
     * @return DTO
     */
    @Override
    public DTO generate(Generate g) {
        if (g == null)
            return new DTO(false).putResult("100", "生成代码失败！");

        if (!pathExist(g.getWsPath() + g.getpPath()))
            return new DTO(false).putResult("101", "工程路径不存在");

        // 取表
        DTO trd = tableService.getTable(g.getTableName());
        Table table;
        if (trd == null || !trd.isSuccess() || (table = trd.getObject()) == null)
            return new DTO(false).putResult("102", "表不存在！");
        g.setTable(table);

        // 初始化
        if (!this.generateInit(g))
            return new DTO(false).putResult("100", "生成代码失败！");

        // 写入空行到日志
        this.writeLog(g.getLogFile(), "");

        // provide
        if (g.getProvide_().isTrue())
            this.generateProvide(g);

        // dao
        if (g.getDao_().isTrue())
            this.generateDao(g);

        // service
        if (g.getService_().isTrue()) {
            this.generateService(g);
        }

        // web
        if (g.getWeb_().isTrue())
            this.generateWeb(g);

        return new DTO(true);
    }

    private boolean generateInit(Generate g) {
        if (g == null || g.getTable() == null)
            return false;

        // 日志
        g.setLogFile(new File(g.getWsPath() + g.getpPath() + "/cglog.log"));

        // module path
        this.modulePath(g);

        // 表信息
        this.fillPojo(g.getTable(), g.getPojoColumn(), g.getPojoField(), g.getPojoType());

        // idType
        this.idType(g);

        // 类信息
        this.fillClass(g);


        return true;
    }

    /**
     * @param table   table
     * @param columns columns
     * @param fields  fields
     * @param types   types
     */
    private void fillPojo(Table table, String[] columns, String[] fields, String[] types) {
        if (table == null || table.getColumns() == null
                || columns == null || table.getColumns().size() != columns.length
                || fields == null || table.getColumns().size() != fields.length
                || types == null || table.getColumns().size() != types.length)
            return;

        Column c;
        for (int i = 0; i < table.getColumns().size(); i++) {
            if ((c = table.getColumns().get(i)) == null
                    || !c.getField().equalsIgnoreCase(columns[i]))
                continue;

            // column type and length
            Matcher matcher = TYPE_LENGTH_PATTERN.matcher(c.getType());
            if (matcher.matches() && matcher.groupCount() == 4) {
                if (matcher.group(1) != null)
                    c.setColumnType(matcher.group(1));
                if (matcher.group(3) != null)
                    c.setColumnLength(matcher.group(3));
            }

            c.setField_(fields[i]);
            c.setType_(types[i]);
            c.setNull_(true);

            if ("NO".equals(c.getIsNull())) {
                c.setNull_(false);
            }

            if ("PRI".equalsIgnoreCase(c.getKey()) && StringUtils.containsIgnoreCase(c.getField(), "ID")) {
                c.setId_(true);
                c.setIdType_("INT".equalsIgnoreCase(types[i]) ? "Integer" : "Long");

                c.setMybatisValue("NULL");
            }

            if ("DATETIME".equalsIgnoreCase(c.getType())
                    && StringUtils.containsIgnoreCase(c.getField(), "_create")) {
                c.setCm_(true);

                c.setMybatisValue("NOW()");
            }

            if ("TIMESTAMP".equalsIgnoreCase(c.getType())
                    && StringUtils.containsIgnoreCase(c.getField(), "_modify")) {
                c.setCm_(true);

                c.setMybatisValue("NULL");
            }
        }
    }

    /**
     * @param g g
     */
    private void idType(Generate g) {
        if (g == null)
            return;

        g.setIdType("");

        if (g.getTable() != null && g.getTable().getIdColumn() != null)
            g.setIdType(g.getTable().getIdColumn().getIdType_());
    }

    /**
     * @param g g
     */
    private void fillClass(Generate g) {
        if (g == null)
            return;

        // name & reference & veriable
        this.nrv(g);

        // class mapping
        this.classMapping(g);
    }

    /**
     * name & reference & veriable
     *
     * @param g g
     */
    private void nrv(Generate g) {
        if (g == null)
            return;

        g.setProvideOReference(g.getProvideOPackage() + "." + g.getProvideO());
        g.setProvideOVeriable(StringUtils.uncapitalize(g.getProvideO()));
        g.setProvideProvideReference(g.getProvideProvidePackage() + "." + g.getProvideProvide());
        g.setProvideProvideVeriable(StringUtils.uncapitalize(StringUtils.substring(g.getProvideProvide(), 1)));

        g.setDaoPOReference(g.getDaoPOPackage() + "." + g.getDaoPO());
        g.setDaoPOVeriable(StringUtils.uncapitalize(g.getDaoPO()));
        g.setDaoMapperReference(g.getDaoMapperPackage() + "." + g.getDaoMapper());
        g.setDaoMapperVeriable(StringUtils.uncapitalize(StringUtils.substring(g.getDaoMapper(), 1)));
        g.setDaoJpaReference(g.getDaoJpaPackage() + "." + g.getDaoJpa());
        g.setDaoJpaVeriable(StringUtils.uncapitalize(StringUtils.substring(g.getDaoJpa(), 1)));
        g.setDaoDAOReference(g.getDaoDAOPackage() + "." + g.getDaoDAO());
        g.setDaoDAOVeriable(StringUtils.uncapitalize(StringUtils.substring(g.getDaoDAO(), 1)));
        g.setDaoDAOImplReference(g.getDaoDAOImplPackage() + "." + g.getDaoDAOImpl());

        g.setDaoBaseTestPackage(g.getpPackage() + ".dao");
        g.setDaoBaseTest("BaseDAOTest");
        g.setDaoBaseTestReference(g.getDaoBaseTestPackage() + "." + g.getDaoBaseTest());
        g.setDaoDAOTestPackage(g.getDaoDAOTestPackage());
        g.setDaoDAOTest(g.getDaoDAOTest());

        g.setServiceDOReference(g.getServiceDOPackage() + "." + g.getServiceDO());
        g.setServiceDOVeriable(StringUtils.uncapitalize(g.getServiceDO()));
        g.setServiceServiceReference(g.getServiceServicePackage() + "." + g.getServiceService());
        g.setServiceServiceVeriable(StringUtils.uncapitalize(StringUtils.substring(g.getServiceService(), 1)));
        g.setServiceServiceImplReference(g.getServiceServiceImplPackage() + "." + g.getServiceServiceImpl());

        g.setServiceBaseTestPackage(g.getpPackage() + ".service");
        g.setServiceBaseTest("BaseServiceTest");
        g.setServiceBaseTestReference(g.getServiceBaseTestPackage() + "." + g.getServiceBaseTest());
        g.setServiceServiceTestPackage(g.getServiceServiceTestPackage());
        g.setServiceServiceTest(g.getServiceServiceTest());

        g.setWebVOReference(g.getWebVOPackage() + "." + g.getWebVO());
        g.setWebVOVeriable(StringUtils.uncapitalize(g.getWebVO()));
        g.setWebControllerReference(g.getWebControllerPackage() + "." + g.getWebController());
        g.setWebControllerVeriable(StringUtils.uncapitalize(StringUtils.substring(g.getWebController(), 1)));

        g.setWebAbstractControllerPackage(g.getpPackage() + ".web.controller");
        g.setWebAbstractController("AbstractController");
        g.setWebAbstractControllerReference(g.getWebAbstractControllerPackage() + "." + g.getWebAbstractController());
    }

    /**
     * @param g g
     */
    private void classMapping(Generate g) {
        if (g == null)
            return;

        CLASS_MAPPING.put(g.getProvideO(), g.getProvideOReference());
        CLASS_MAPPING.put(g.getProvideProvide(), g.getProvideProvideReference());

        CLASS_MAPPING.put(g.getDaoPO(), g.getDaoPOReference());
        CLASS_MAPPING.put(g.getDaoMapper(), g.getDaoMapperReference());
        CLASS_MAPPING.put(g.getDaoJpa(), g.getDaoJpaReference());
        CLASS_MAPPING.put(g.getDaoDAO(), g.getDaoDAOReference());
        CLASS_MAPPING.put(g.getDaoDAOImpl(), g.getDaoDAOImplReference());
        CLASS_MAPPING.put(g.getDaoBaseTest(), g.getDaoBaseTestReference());

        CLASS_MAPPING.put(g.getServiceDO(), g.getServiceDOReference());
        CLASS_MAPPING.put(g.getServiceService(), g.getServiceServiceReference());
        CLASS_MAPPING.put(g.getServiceServiceImpl(), g.getServiceServiceImplReference());
        CLASS_MAPPING.put(g.getServiceBaseTest(), g.getServiceBaseTestReference());

        CLASS_MAPPING.put(g.getWebVO(), g.getWebVOReference());
        CLASS_MAPPING.put(g.getWebController(), g.getWebControllerReference());

        CLASS_MAPPING.put(g.getWebAbstractController(), g.getWebAbstractControllerReference());
    }

    /**
     * @return boolean
     */
    private boolean pathExist(String path) {
        if (!new File(path).exists())
            return false;
        return true;
    }

    /**
     * @param g g
     */
    private void modulePath(Generate g) {
        String path = g.getWsPath() + g.getpPath();

        g.setProvideModuleFile(new File(path + g.getProvideModule()));
        g.setDaoModuleFile(new File(path + g.getDaoModule()));
        g.setServiceModuleFile(new File(path + g.getServiceModule()));
        g.setWebModuleFile(new File(path + g.getWebModule()));
    }

    /**
     * @param imports
     * @param className
     */
    private void mappingImport(Set<String> imports, String className) {
        if (imports == null || className == null)
            return;

        String name = CLASS_MAPPING.get(className);
        if (name != null && name.length() > 0)
            imports.add(name);
    }

    /**
     * @param cxt      cxt
     * @param imports  imports
     * @param author   author
     * @param _package package
     * @param name     name
     */
    private void baseVariable(Context cxt, Set<String> imports,
                              String author, String _package, String name) {
        cxt.clearVariables();
        imports.clear();

        cxt.setVariable("imports", imports);
        cxt.setVariable("date", new Date());
        cxt.setVariable("author", author);

        cxt.setVariable("package", _package);
        cxt.setVariable("name", name);
    }

    /**
     * @param g g
     */
    private void generateProvide(Generate g) {

        Context cxt = new Context();
        Set<String> imports = new LinkedHashSet<>();

        // o
        if (g.getProvideO_().isTrue()) {
            baseVariable(cxt, imports, g.getAuthor(), g.getProvideOPackage(), g.getProvideO());
            mappingImport(imports, "Serializable");

            List<String> oFields = new ArrayList<>();
            List<String> oFieldComments = new ArrayList<>();
            List<CgField> oGetterAndSetters = new ArrayList<>();

            g.getTable().getColumns().forEach(c -> {
                if (c.isCm_())
                    return;

                mappingImport(imports, c.getType_());

                if (c.getComment() != null && c.getComment().length() > 0)
                    oFieldComments.add("// " + c.getComment());
                else
                    oFieldComments.add("");
                oFields.add(String.format("private %s %s;", c.getType_(), c.getField_()));

                oGetterAndSetters.add(cgField(c));
            });

            cxt.setVariable("oFields", oFields);
            cxt.setVariable("oFieldComments", oFieldComments);
            cxt.setVariable("oGetterAndSetter", oGetterAndSetters);

            this.writeFile(g.getLogFile(),
                    package2path(g.getProvideModuleFile().getPath(), JAVA_PATH, g.getProvideOPackage()),
                    g.getProvideO(), JAVA_EXT_NAME,
                    process(O_TXT_TEMPLATE_NAME, cxt));
        }

        if (g.getProvideProvide_().isTrue()) {
            // provide
            baseVariable(cxt, imports, g.getAuthor(), g.getProvideProvidePackage(), g.getProvideProvide());

            this.writeFile(g.getLogFile(),
                    package2path(g.getProvideModuleFile().getPath(), JAVA_PATH, g.getProvideProvidePackage()),
                    g.getProvideProvide(), JAVA_EXT_NAME,
                    process(PROVIDE_TXT_TEMPLATE_NAME, cxt));
        }
    }

    /**
     * @param resoucesPath modulePath
     * @param resoucesPath resoucesPath
     * @param _package     _package
     * @return
     */
    private String package2path(String modulePath, String resoucesPath, String _package) {
        return StringUtils.trimToEmpty(modulePath)
                + "/"
                + StringUtils.trimToEmpty(resoucesPath)
                + _package.replace('.', '/');
    }

    /**
     * @param c c
     */
    private CgField cgField(Column c) {
        CgField field = new CgField();

        if (c == null)
            return field;

        field.setType(c.getType_());
        field.setField(c.getField_());
        field.setName(StringUtils.capitalize(c.getField_()));

        return field;
    }


    /**
     * @param g g
     */
    private void generateDao(Generate g) {

        Context cxt = new Context();
        Set<String> imports = new LinkedHashSet<>();

        if (g.getDaoPO_().isTrue()) {
            // po
            baseVariable(cxt, imports, g.getAuthor(), g.getDaoPOPackage(), g.getDaoPO());

            mappingImport(imports, "Entity");
            mappingImport(imports, "Table");
            mappingImport(imports, "Column");

            // po field
            List<String> poFields = new ArrayList<>();
            List<CgField> poGetterAndSetters = new ArrayList<>();

            g.getTable().getColumns().forEach(c -> {
                mappingImport(imports, c.getType_());

                CgField field = cgField(c);
                field.setAnnotations(new LinkedHashSet<>());

                // annotations
                if (c.isId_()) {
                    mappingImport(imports, "ID");
                    mappingImport(imports, "GeneratedValue");
                    mappingImport(imports, "GenerationType");

                    field.getAnnotations().add("@Id");
                    field.getAnnotations().add("@GeneratedValue(strategy = GenerationType.IDENTITY)");
                }

                String colAnno = "name = \"" + c.getField() + "\"";
                if (c.isNull_())
                    colAnno += ", nullable = true";
                else
                    colAnno += ", nullable = false";

                if (c.getColumnLength() != null)
                    colAnno += ", length = " + c.getColumnLength();

                if (c.isCm_()) {
                    colAnno += ", insertable = false, updatable = false";
                    String columnDefinition = c.getType_().toUpperCase();
                    if (c.getDefaultValue() != null && c.getDefaultValue().length() > 0) {
                        if (columnDefinition.length() != 0)
                            columnDefinition += " ";
                        columnDefinition += "DEFAULT " + c.getDefaultValue().toUpperCase();
                    }

                    if (c.getExtra() != null && c.getExtra().length() > 0) {
                        if (columnDefinition.length() != 0)
                            columnDefinition += " ";

                        columnDefinition += c.getExtra().toUpperCase();
                    }
                    colAnno += ", columnDefinition = \"" + columnDefinition + "\"";
                }
                field.getAnnotations().add("@Column(" + colAnno + ")");

                poFields.add(String.format("private %s %s;", c.getType_(), c.getField_()));
                cxt.setVariable("poFields", poFields);

                poGetterAndSetters.add(field);
                cxt.setVariable("poGetterAndSetter", poGetterAndSetters);
            });

            cxt.setVariable("tableName", g.getTable().getName());

            this.writeFile(g.getLogFile(),
                    package2path(g.getDaoModuleFile().getPath(), JAVA_PATH, g.getDaoPOPackage()),
                    g.getDaoPO(), JAVA_EXT_NAME,
                    process(PO_TXT_TEMPLATE_NAME, cxt));
        }

        if (g.getDaoMapper_().isTrue()) {
            // mapper
            baseVariable(cxt, imports, g.getAuthor(), g.getDaoMapperPackage(), g.getDaoMapper());

            mappingImport(imports, "IMapper");
            mappingImport(imports, g.getDaoPO());

            cxt.setVariable("poName", g.getDaoPO());
            cxt.setVariable("idType", g.getIdType());

            this.writeFile(g.getLogFile(),
                    package2path(g.getDaoModuleFile().getPath(), JAVA_PATH, g.getDaoMapperPackage()),
                    g.getDaoMapper(), JAVA_EXT_NAME,
                    process(MAPPER_TXT_TEMPLATE_NAME, cxt));
            // end of mapper

            // mapper xml
            baseVariable(cxt, imports, g.getAuthor(), g.getDaoMapperPackage(), g.getDaoMapper());

            List<String> resultMap = new ArrayList<>();
            List<String> poInsertColumns = new ArrayList<>();
            List<String> poInsertFields = new ArrayList<>();

            List<String> poUpdateColumns = new ArrayList<>();
            List<String> poUpdateFields = new ArrayList<>();

            Column idc = g.getTable().getIdColumn();
            final String[] tableColumnsStr = {""};
            final String[] idColumn = {idc != null ? idc.getField() : null};
            final String[] idFieldName = {idc != null ? idc.getField_() : null};
            final String[] idField = {idc != null ? "#{" + idc.getField_() + "}" : null};

            g.getTable().getColumns().forEach(c -> {
                if (tableColumnsStr[0].length() > 0)
                    tableColumnsStr[0] += ", ";
                tableColumnsStr[0] += c.getField();

                poInsertColumns.add(c.getField());

                if (c.isId_()) {
                    resultMap.add("<id property=\"" + c.getField_() + "\" column=\"" + c.getField() + "\" />");

                    poInsertFields.add(c.getMybatisValue());
                } else {
                    resultMap.add("<result property=\"" + c.getField_() + "\" column=\"" + c.getField() + "\" />");

                    if (c.isCm_()) {
                        poInsertFields.add(c.getMybatisValue());
                    } else {
                        poInsertFields.add("#{" + c.getField_() + "}");

                        poUpdateColumns.add(c.getField());
                        poUpdateFields.add("#{" + c.getField_() + "}");
                    }
                }
            });

            cxt.setVariable("tableName", g.getTable().getName());
            cxt.setVariable("namespace", g.getDaoMapperReference());

            cxt.setVariable("poName", g.getDaoPO());
            cxt.setVariable("idType", g.getIdType());

            cxt.setVariable("idColumn", idColumn[0]);
            cxt.setVariable("idFieldName", idFieldName[0]);
            cxt.setVariable("idField", idField[0]);
            cxt.setVariable("resultMap", resultMap);
            cxt.setVariable("tableColumnsStr", tableColumnsStr[0]);
            cxt.setVariable("poInsertColumns", poInsertColumns);
            cxt.setVariable("poInsertFields", poInsertFields);
            cxt.setVariable("poUpdateColumns", poUpdateColumns);
            cxt.setVariable("poUpdateFields", poUpdateFields);

            this.writeFile(g.getLogFile(),
                    package2path(g.getDaoModuleFile().getPath(), JAVA_PATH, g.getDaoMapperPackage()),
                    g.getDaoMapper(), XML_EXT_NAME,
                    process(MAPPERXML_TXT_TEMPLATE_NAME, cxt));
            // end of mapper xml
        }

        if (g.getDaoJpa_().isTrue()) {
            // jpa
            baseVariable(cxt, imports, g.getAuthor(), g.getDaoJpaPackage(), g.getDaoJpa());

            mappingImport(imports, "IJpa");
            mappingImport(imports, g.getDaoPO());

            cxt.setVariable("poName", g.getDaoPO());
            cxt.setVariable("idType", g.getIdType());

            this.writeFile(g.getLogFile(),
                    package2path(g.getDaoModuleFile().getPath(), JAVA_PATH, g.getDaoJpaPackage()),
                    g.getDaoJpa(), JAVA_EXT_NAME,
                    process(JPA_TXT_TEMPLATE_NAME, cxt));
        }

        if (g.getDaoDAO_().isTrue()) {
            // dao
            baseVariable(cxt, imports, g.getAuthor(), g.getDaoDAOPackage(), g.getDaoDAO());

            mappingImport(imports, "IDAO");
            mappingImport(imports, g.getDaoPO());

            cxt.setVariable("poName", g.getDaoPO());
            cxt.setVariable("idType", g.getIdType());

            this.writeFile(g.getLogFile(),
                    package2path(g.getDaoModuleFile().getPath(), JAVA_PATH, g.getDaoDAOPackage()),
                    g.getDaoDAO(), JAVA_EXT_NAME,
                    process(DAO_TXT_TEMPLATE_NAME, cxt));
        }

        if (g.getDaoDAOImpl_().isTrue()) {
            // dao mapper&jpa impl
            baseVariable(cxt, imports, g.getAuthor(), g.getDaoDAOImplPackage(), g.getDaoDAOImpl());

            cxt.setVariable("poName", g.getDaoPO());
            cxt.setVariable("idType", g.getIdType());

            cxt.setVariable("daoName", g.getDaoDAO());
            cxt.setVariable("mapperName", g.getDaoMapper());
            cxt.setVariable("jpaName", g.getDaoJpa());

            cxt.setVariable("mapperVeriable", g.getDaoMapperVeriable());
            cxt.setVariable("jpaVeriable", g.getDaoJpaVeriable());

            imports.clear();
            mappingImport(imports, g.getDaoMapper());
            mappingImport(imports, "MapperSupport");
            mappingImport(imports, g.getDaoPO());
            mappingImport(imports, g.getDaoDAO());
            mappingImport(imports, "stereotype.Service");

            cxt.setVariable("name", StringUtils.capitalize(g.getDaoMapperVeriable()));
            this.writeFile(g.getLogFile(),
                    package2path(g.getDaoModuleFile().getPath(), JAVA_PATH, g.getDaoDAOImplPackage()),
                    StringUtils.capitalize(g.getDaoMapperVeriable()), JAVA_EXT_NAME,
                    process(DAOMAPPERIMPL_TXT_TEMPLATE_NAME, cxt));

            imports.clear();
            mappingImport(imports, g.getDaoJpa());
            mappingImport(imports, "JpaSupport");
            mappingImport(imports, g.getDaoPO());
            mappingImport(imports, g.getDaoDAO());
            mappingImport(imports, "stereotype.Service");

            cxt.setVariable("name", StringUtils.capitalize(g.getDaoJpaVeriable()));
            this.writeFile(g.getLogFile(),
                    package2path(g.getDaoModuleFile().getPath(), JAVA_PATH, g.getDaoDAOImplPackage()),
                    StringUtils.capitalize(g.getDaoJpaVeriable()), JAVA_EXT_NAME,
                    process(DAOJPAIMPL_TXT_TEMPLATE_NAME, cxt));
        }

        if (g.getDaoDAOTest_().isTrue()) {
            // dao test
            baseVariable(cxt, imports, g.getAuthor(), g.getDaoDAOTestPackage(), g.getDaoDAOTest());

            baseTestImport(imports);

            mappingImport(imports, "Qualifier");
            mappingImport(imports, g.getDaoPO());
            mappingImport(imports, g.getDaoBaseTest());
            mappingImport(imports, g.getDaoDAO());

            cxt.setVariable("daoName", g.getDaoDAO());
            cxt.setVariable("mapperVeriable", g.getDaoMapperVeriable());
            cxt.setVariable("jpaVeriable", g.getDaoJpaVeriable());

            cxt.setVariable("poName", g.getDaoPO());
            cxt.setVariable("idType", g.getIdType());

            this.testSetter(g, cxt);

            this.writeFile(g.getLogFile(),
                    package2path(g.getDaoModuleFile().getPath(), TEST_PATH, g.getDaoDAOTestPackage()),
                    g.getDaoDAOTest(), JAVA_EXT_NAME,
                    process(DAO_TEST_TXT_TEMPLATE_NAME, cxt));
        }
    }

    /**
     * @param imports imports
     */
    private void baseTestImport(Set<String> imports) {
        mappingImport(imports, "test.Test");
        mappingImport(imports, "DTO");
        mappingImport(imports, "NPage");
        mappingImport(imports, "NSort");
        mappingImport(imports, "Autowired");

        mappingImport(imports, "ArrayList");
        mappingImport(imports, "Arrays");
        mappingImport(imports, "List");
    }

    /**
     * @param g g
     */
    private void generateService(Generate g) {

        Context cxt = new Context();
        Set<String> imports = new LinkedHashSet<>();

        if (g.getServiceDO_().isTrue()) {
            // DO
            baseVariable(cxt, imports, g.getAuthor(), g.getServiceDOPackage(), g.getServiceDO());

            mappingImport(imports, g.getDaoPO());

            cxt.setVariable("poName", g.getDaoPO());

            this.writeFile(g.getLogFile(),
                    package2path(g.getServiceModuleFile().getPath(), JAVA_PATH, g.getServiceDOPackage()),
                    g.getServiceDO(), JAVA_EXT_NAME,
                    process(DO_TXT_TEMPLATE_NAME, cxt));
            // end of DO
        }

        if (g.getServiceService_().isTrue()) {
            // service
            baseVariable(cxt, imports, g.getAuthor(), g.getServiceServicePackage(), g.getServiceService());

            mappingImport(imports, "IService");
            mappingImport(imports, g.getDaoPO());
            mappingImport(imports, g.getServiceDO());
            mappingImport(imports, g.getProvideProvide());

            cxt.setVariable("poName", g.getDaoPO());
            cxt.setVariable("doName", g.getServiceDO());
            cxt.setVariable("idType", g.getIdType());
            cxt.setVariable("provideName", g.getProvideProvide());

            this.writeFile(g.getLogFile(),
                    package2path(g.getServiceModuleFile().getPath(), JAVA_PATH, g.getServiceServicePackage()),
                    g.getServiceService(), JAVA_EXT_NAME,
                    process(SERVICE_TXT_TEMPLATE_NAME, cxt));
            // end of service
        }

        if (g.getServiceServiceImpl_().isTrue()) {
            // service impl
            baseVariable(cxt, imports, g.getAuthor(), g.getServiceServiceImplPackage(), g.getServiceServiceImpl());

            mappingImport(imports, g.getDaoDAO());
            mappingImport(imports, g.getDaoPO());
            mappingImport(imports, g.getServiceDO());
            mappingImport(imports, g.getServiceService());
            mappingImport(imports, "ServiceSupport");
            mappingImport(imports, "Qualifier");
            mappingImport(imports, "stereotype.Service");

            cxt.setVariable("poName", g.getDaoPO());
            cxt.setVariable("doName", g.getServiceDO());
            cxt.setVariable("idType", g.getIdType());
            cxt.setVariable("daoName", g.getDaoDAO());
            cxt.setVariable("serviceName", g.getServiceService());

            cxt.setVariable("mapperVeriable", g.getDaoMapperVeriable());
            cxt.setVariable("jpaVeriable", g.getDaoJpaVeriable());

            this.writeFile(g.getLogFile(),
                    package2path(g.getServiceModuleFile().getPath(), JAVA_PATH, g.getServiceServiceImplPackage()),
                    g.getServiceServiceImpl(), JAVA_EXT_NAME,
                    process(SERVICEIMPL_TXT_TEMPLATE_NAME, cxt));
            // end of service impl
        }

        if (g.getServiceServiceTest_().isTrue()) {
            // service test
            baseVariable(cxt, imports, g.getAuthor(), g.getServiceServiceTestPackage(), g.getServiceServiceTest());

            baseTestImport(imports);

            mappingImport(imports, "Optional");
            mappingImport(imports, g.getDaoPO());
            mappingImport(imports, g.getServiceDO());
            mappingImport(imports, g.getServiceBaseTest());

            cxt.setVariable("poName", g.getDaoPO());
            cxt.setVariable("doName", g.getServiceDO());
            cxt.setVariable("idType", g.getIdType());
            cxt.setVariable("serviceName", g.getServiceService());
            cxt.setVariable("serviceVeriable", g.getServiceServiceVeriable());

            this.testSetter(g, cxt);

            this.writeFile(g.getLogFile(),
                    package2path(g.getServiceModuleFile().getPath(), TEST_PATH, g.getServiceServiceTestPackage()),
                    g.getServiceServiceTest(), JAVA_EXT_NAME,
                    process(SERVICETEST_TXT_TEMPLATE_NAME, cxt));
            // end of service test
        }
    }

    /**
     * @param g   g
     * @param cxt cxt
     */
    private void testSetter(Generate g, Context cxt) {
        List<String> poSetters = new ArrayList<>();
        g.getTable().getColumns().forEach(c -> {
            if (c.isCm_())
                return;

            poSetters.add("// po.set" + StringUtils.capitalize(c.getField_()) + "(\"" + c.getField_() + "\");");
        });
        cxt.setVariable("poSetters", poSetters);
    }

    /**
     * @param g g
     */
    private void generateWeb(Generate g) {

        Context cxt = new Context();
        Set<String> imports = new LinkedHashSet<>();

        if (g.getWebVO_().isTrue()) {
            // vo
            baseVariable(cxt, imports, g.getAuthor(), g.getWebVOPackage(), g.getWebVO());

            mappingImport(imports, g.getServiceDO());

            cxt.setVariable("doName", g.getServiceDO());

            this.writeFile(g.getLogFile(),
                    package2path(g.getWebModuleFile().getPath(), JAVA_PATH, g.getWebVOPackage()),
                    g.getWebVO(), JAVA_EXT_NAME,
                    process(VO_TXT_TEMPLATE_NAME, cxt));
            // end of vo
        }

        if (g.getWebController_().isTrue()) {
            // controller
            baseVariable(cxt, imports, g.getAuthor(), g.getWebControllerPackage(), g.getWebController());

            mappingImport(imports, "DTO");
            mappingImport(imports, "NPage");
            mappingImport(imports, "NSort");

            mappingImport(imports, "MediaType");
            mappingImport(imports, "Controller");
            mappingImport(imports, "PathVariable");
            mappingImport(imports, "RequestMapping");
            mappingImport(imports, "RequestMethod");
            mappingImport(imports, "RequestParam");
            mappingImport(imports, "ResponseBody");
            mappingImport(imports, "ModelAndView");

            mappingImport(imports, "Map");

            mappingImport(imports, g.getWebVO());
            mappingImport(imports, g.getServiceService());
            mappingImport(imports, g.getWebAbstractController());

            cxt.setVariable("idType", g.getIdType());
            cxt.setVariable("tableName", g.getTable().getName());
            cxt.setVariable("serviceName", g.getServiceService());
            cxt.setVariable("serviceVeriable", g.getServiceServiceVeriable());
            cxt.setVariable("webVO", g.getWebVO());
            cxt.setVariable("webVOVeriable", g.getWebVOVeriable());

            this.writeFile(g.getLogFile(),
                    package2path(g.getWebModuleFile().getPath(), JAVA_PATH, g.getWebControllerPackage()),
                    g.getWebController(), JAVA_EXT_NAME,
                    process(CONTROLLER_TXT_TEMPLATE_NAME, cxt));
            // end of controller
        }
    }

    private String process(String template, IContext context) {
        return templateEngine().process(template, context);
    }


    public TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();

        templateResolver.setTemplateMode(TemplateMode.TEXT);
        templateResolver.setPrefix("/template/");
        templateResolver.setSuffix(".txt");
        templateResolver.setCacheTTLMs(3600000L);
        templateResolver.setCacheable(false);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        return templateEngine;
    }

    /**
     * @param pPath
     * @param content
     */
    private void writeFile(File logFile, String pPath, String name, String ext, String content) {
        if (isBlank(pPath))
            return;
        if (content == null)
            content = "";

        File pathFile = new File(pPath);
        if (!pathFile.exists())
            pathFile.mkdirs();

        String fileName = pPath + "/" + name + ext;
        File file = new File(fileName);
        if (file.exists() && !overwrite)
            // 文件存在且不覆盖文件，在文件名前加"_"
            fileName = pPath + "/" + "_" + name + ext;

        // write log
        writeLog(logFile, fileName);

        FileWriter fw = null;
        try {
            fw = new FileWriter(fileName);
            fw.write(content, 0, content.length());
            fw.flush();
        } catch (IOException e) {
            logger.error("", e);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    logger.error("", e);
                }
            }
        }
    }

    /**
     * @param logFile
     * @param line
     */
    private void writeLog(File logFile, String line) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(logFile, true);
            fw.write(line + "\n");
            fw.flush();
        } catch (IOException e) {
            logger.error("", e);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    logger.error("", e);
                }
            }
        }
    }

    /**
     * @param str
     * @return
     */
    private boolean isBlank(String str) {
        return StringUtils.isBlank(str);
    }
}