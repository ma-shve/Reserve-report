import java.io.*;
import java.util.ArrayList;
import java.util.Properties;

public class Config {

    public static ArrayList<String> read() {
        ArrayList<String> result = new ArrayList<String>();
        FileInputStream fis = null;
        InputStreamReader reader = null;
        try {
            fis = new FileInputStream("src\\main\\resources\\config.properties");
            reader = new InputStreamReader(fis,"Cp1251");
            Properties property = new Properties();
            property.load(reader);
            result.add(property.getProperty("logConfigPath"));      // 0
            result.add(property.getProperty("requestPath"));        // 1
            result.add(property.getProperty("planPath"));           // 2
            result.add(property.getProperty("reservePath"));        // 3
            result.add(property.getProperty("planSheetName"));      // 4
            result.add(property.getProperty("reserveSheetName"));   // 5
            result.add(property.getProperty("unitColName"));        // 6
            result.add(property.getProperty("aggrUnitColName"));    // 7
            result.add(property.getProperty("reportPath"));         // 8
            result.add(property.getProperty("serverName"));         // 9
            result.add(property.getProperty("dbName"));             // 10
            result.add(property.getProperty("userName"));           // 11
            result.add(property.getProperty("pass"));               // 12
            result.add(property.getProperty("serverConfig"));       // 13
            result.add(property.getProperty("structurePath"));      // 14
            result.add(property.getProperty("structureSheetName")); // 15
            result.add(property.getProperty("shortNamesPath"));     // 16
            result.add(property.getProperty("shortNamesSheetName"));// 17
            fis.close();
            reader.close();
        } catch (IOException e) {
            System.err.println("ОШИБКА: Файл конфигураций отсуствует!");
        }
        return result;
    }
}
