import org.apache.poi.ss.formula.functions.Column;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.io.File;
import java.io.FileInputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ChangeData {
    static Logger LOGGER;
    static {
        //cfg.get(0) - путь к файлу конфигурации логера
        try(FileInputStream ins = new FileInputStream(Main.cfg.get(0) )){
            LogManager.getLogManager().readConfiguration(ins);
            LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static HashMap<String, String> setNameMap (String key){
        HashMap<String, String> map = new HashMap<>();
        switch (key){
        case "names":
            map.put("Алтуфьево" ,"ЧиА"); //Алтуфьево
            map.put("Челомея" ,"ЧиА"); //Челомея
            map.put("Остальное" ,"ЧиА"); //Остальное
            map.put("интернет магазин","ИМ"); //интернет магазин
            map.put("дилерский отдел" ,"Дилер"); //дилерский отдел
            map.put("Новосибирск" ,"НСК"); //Новосибирск
            map.put("Питер" ,"СПб"); //Питер
        }
        return map;
    }

    public static void getTableArray (JTable table, ArrayList<String> headerArray, ArrayList<ArrayList> tableArray){
        int colCount = table.getColumnCount();
        int rowCount = table.getRowCount();
        for (int col = 0; col < colCount; col++){
            headerArray.add(table.getColumnName(col));
            tableArray.add( new ArrayList<>() );
            for (int row = 0; row < rowCount; row++){
                tableArray.get(col).add(table.getValueAt(row,col));
            }
        }
    }

    public static HashMap<String, String> getNameMap (String fileName, String sheetName) {
        try{
            HashMap<String, String> result = new HashMap<>();
            JTable filialTable = ExcelAPI.read(new File(fileName), sheetName);
            DefaultTableModel filialableModel = (DefaultTableModel) filialTable.getModel();
            Vector filialTableVector = filialableModel.getDataVector();
            for(int i = 0; i < filialTableVector.size(); i++){
                if ( filialTableVector.get(i).toString().equals(null) || filialTableVector.get(i).toString().equals("")){
                    LOGGER.log(Level.WARNING,"Внимание! Формат данных не соответствует установленному, найдены пропуски значений или пустые строки.");
                    System.exit(1);
                }
                String[] splitArr = filialTableVector.get(i).toString().split(",");
                String value = splitArr[1].substring(1).replace("]","");
                String key = splitArr[0].replace("[","");
                result.put(key, value);
            }
            return result;
        }catch (Exception e){
            LOGGER.log(Level.WARNING,"Внимание! Файл не найден или формат данных не соответсвтует заданному.");
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }
}
