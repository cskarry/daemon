package com.alibaba.china.talos.quick.tasker;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.alibaba.common.lang.StringUtil;
import com.alibaba.common.logging.Logger;
import com.alibaba.common.logging.LoggerFactory;

public class ITaskControl {

    private static final Logger logger = LoggerFactory.getLogger(ITaskControl.class);
    
    private static final String CLASS_SUFFIX = ".class";
    
    private static final String JAVA_SUFFIX  = ".java";

    private static IClassLoader iClassLoader = new IClassLoader(ITaskControl.class.getClassLoader());
    
    public static void compile(String javaFile) {
        //简单判断是否需要编译
        if (javaFile.endsWith(CLASS_SUFFIX)) {
            return;
        }
        //编译java文件
        if (javaFile.endsWith(JAVA_SUFFIX)) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromStrings(Arrays.asList(javaFile));
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null,
                                                                 compilationUnits);
            boolean success = task.call();
            try {
                fileManager.close();
            } catch (IOException e) {
                logger.error("fileManager close error where javaFile is " + javaFile, e);
            }
            logger.info("compile result is " + success + " where javaFile is:" + javaFile);
        }
        
    }
    
    public static void refreshClassPath(String javaFile) {
        File jFile = new File(javaFile);
        if (jFile.isDirectory()) {
            throw new RuntimeException("javaFile is invalid where javaFile is " + javaFile);
        }
        IClassLoader.refreshContext(StringUtil.trim(jFile.getParent()));
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void invoke(String className) throws Exception {
        Class<ITask> clazz = (Class<ITask>) Class.forName(className, false, iClassLoader);
        Object obj = clazz.newInstance();
        Method method = clazz.getMethod("execute");
        method.invoke(obj);
    }

}
