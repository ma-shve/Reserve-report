import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.FileInputStream;
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class JTableAPI {
    static Logger LOGGER;
    static {
        //cfg.get(0) - путь к файлу конфигурации логера
        try(FileInputStream ins = new FileInputStream(Main.cfg.get(0))){
            LogManager.getLogManager().readConfiguration(ins);
            LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static DefaultTableModel buildTableModel_SQL(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        // Записываем в вектор заголовки
        Vector<String> columnNames = new Vector<String>();
        int columnCount = metaData.getColumnCount();
        for (int column = 1; column <= columnCount; column++) {
            columnNames.add(metaData.getColumnName(column));
        }
        // Записываем в вектор масив векторов с данными
        Vector<Vector<Object>> data = new Vector<Vector<Object>>();
        while (rs.next()) {
            Vector<Object> vector = new Vector<Object>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                vector.add(rs.getObject(columnIndex));
            }
            data.add(vector);
        }
        return new DefaultTableModel(data, columnNames);
    }

    public static DefaultTableModel buildTableModel_Array(ArrayList<String> columnNameList, ArrayList<ArrayList> columnList){
        // Записываем в вектор масив заголовков
        Vector<String> columnNames = new Vector<String>();
        int columnCount = columnNameList.size();
        //System.out.println("Заголовков для записи: " + columnCount);
        for (int column = 0; column < columnCount; column++) {
            columnNames.add(columnNameList.get(column));

        }
        // Записываем в вектор масив векторов с данными
        Vector<Vector<Object>> data = new Vector<Vector<Object>>();
        int rowCount = columnList.get(0).size();
        for (int row = 0; row < rowCount; row++){
            Vector<Object> vector = new Vector<Object>();
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                 try{
                    vector.add(columnList.get(columnIndex).get(row));
                 }
                 catch (IndexOutOfBoundsException e) {
                    vector.add("");
                }
            }
            data.add(vector);
        }
        return new DefaultTableModel(data, columnNames);
    }
    public static JTable addColumn (JTable table, String colName, int colIndex){
        LOGGER.log(Level.INFO,"Добавляем столбец: " + colName);
        ArrayList<String> columnNameList = new ArrayList<String>();
        ArrayList<ArrayList> columnList = new ArrayList<ArrayList>();
        ChangeData.getTableArray(table, columnNameList, columnList);
        columnNameList.add( colIndex, colName );
        columnList.add( colIndex, new ArrayList<>() );
        table = new JTable(JTableAPI.buildTableModel_Array(columnNameList, columnList));
        return table;
    }

    public static JTable fillColumn (JTable table, String parentName, String childName){
        int colCount = table.getColumnCount();
        int rowCount = table.getRowCount();
        int oldCount = 0, newCount = 0;
        for (int i = 0; i < colCount; i++) {
            if ((table.getColumnName(i).toString()).equals(parentName)){ oldCount = i; }
            if ((table.getColumnName(i).toString()).equals(childName)){ newCount = i; }
        }
        //cfg.get(14) - путь к файлу справочнику структуры предприятия
        //cfg.get(15) - Лист расположения справочника в файле
        LOGGER.log(Level.INFO,"Поиск соответствия имен филиалов и подразделений в файле: " + Main.cfg.get(14));
        HashMap<String, String> FilialNameMap = ChangeData.getNameMap (Main.cfg.get(14), Main.cfg.get(15));
        for (int k = 0; k < rowCount; k++) {
            table.setValueAt(FilialNameMap.get(table.getValueAt(k, oldCount).toString()), k, newCount);
        }
        return table;
    }

    public static JTable leftJoin (JTable table1, JTable table2, String colName){
        //Переводим в массив первую таблицу
        ArrayList<String> columnNameList1 = new ArrayList<String>();
        ArrayList<ArrayList> columnList1 = new ArrayList<ArrayList>();
        ChangeData.getTableArray(table1, columnNameList1, columnList1);
        //Переводим в массив вторую таблицу
        ArrayList<String> columnNameList2 = new ArrayList<String>();
        ArrayList<ArrayList> columnList2 = new ArrayList<ArrayList>();
        ChangeData.getTableArray(table2, columnNameList2, columnList2);
        int colCount1 = table1.getColumnCount();
        int colCount2 = table2.getColumnCount();
        int rowCount2 = table2.getRowCount();
        //Добавляем в конец первой таблицы заголовки второй и такое же количество пустых столбцов
        for (int col = 0; col < colCount2; col++){
            if ( !table2.getColumnName(col).equals(colName) ){
                columnNameList1.add(table2.getColumnName(col));
                columnList1.add( new ArrayList<>() );
            }
        }
        //Количество новых столбцов
        int update = table1.getColumnCount() - colCount1;
        //Количество столбцов и строк в таблице после присоединения заголовков
        colCount1 = table1.getColumnCount();
        int rowCount1 = table1.getRowCount();
        int nameColCount1 = 0;
        int nameColCount2 = 0;
        //Номер ключевого столбца в первой и второй таблицах
        for (int col = 0; col < colCount1; col++){
            if (columnNameList1.get(col).equals(colName)) {
                nameColCount1 = col;
            }
        }
        for (int col = 0; col < colCount2; col++){
            if (columnNameList2.get(col).equals(colName)) {
                nameColCount2 = col;
            }
        }
        //Массивы ключевых столбцов
        ArrayList<String> list1 = columnList1.get(nameColCount1);
        ArrayList<String> list2 = columnList2.get(nameColCount2);
        /*for (int i=0; i < list1.size(); i++ ){
            System.out.println(list1.get(i));
        }
        for (int i=0; i < list2.size(); i++ ){
            System.out.println(list2.get(i));
        }
        System.exit(1);*/
        //Обходим во внешнем цикле левую(главную) таблицу по рядам
        //Во внутреннем по рядам обходим правую(присоединяемую)
        for(int row1 = 0; row1 < rowCount1; row1++) {
            for(int row2 = 0; row2 < rowCount2; row2++) {
                //Если значения в ключевых столбцах совпадают
                if (list1.get(row1).equals(list2.get(row2))){
                    //Устанавливаем номер столбца в левой таблице, как общее количество столбцов минус количество столбцов к добавлению
                    int col1 = colCount1 - update;
                    //Для количества столбцов в присоединяемой таблице, добавляем в левую(главную) таблицу значения
                    //В случае, если добавляется больше одного столбца, увеличиаем индекс столбца в левой таблице
                    for (int col2 = nameColCount2 + 1; col2 < colCount2; col2++) {
                        columnList1.get(col1).add(columnList2.get(col2).get(row2));
                        col1++;
                    }
                }
            }
        }
        JTable table = new JTable(JTableAPI.buildTableModel_Array(columnNameList1, columnList1));
        return table;
    }

    public static ArrayList<String> getUniqueList (ArrayList<String> fieldList){
        ArrayList<String> result = new ArrayList<String>();
        for (int i = 0; i < fieldList.size(); i++){
            boolean isUniqueName = true;
            //Если список уникальных имен пуст, записываем значение из столбца в список
            if (result.size() > 0) {
                //Если длина списка уникальных значений больше 0, проверяем i элемент столбца на уникальность
                for(int j = 0; j < result.size(); j++){
                    //Если в списке уникальных значений уже есть такой элемент устанавливаем флаг и выходим из вложенного цикла
                    if (result.get(j).equals(fieldList.get(i))){
                        isUniqueName = false;
                        break;
                    }
                }
                if (isUniqueName){
                    //Если элемент уникален - добавляем его в список
                    result.add(fieldList.get(i));
                }
            }
            else{
                //Если элемент уникален - добавляем его в список
                result.add(fieldList.get(i));
            }
        }
        return result;
    }

    public static HashMap<String, JTable> splitByField (JTable table, String field){
        HashMap<String, JTable> result = new HashMap<String, JTable>();
        int colCount = table.getColumnCount();
        int rowCount = table.getRowCount();
        //Переводим в массив таблицу table
        ArrayList<String> columnNameList = new ArrayList<String>();
        ArrayList<ArrayList> columnList = new ArrayList<ArrayList>();
        ChangeData.getTableArray(table, columnNameList, columnList);
        //Номер ключевого поля в таблице
        int nameColCount = 0;
        for (int col = 0; col < colCount; col++){
            //Что делать если есть несколько одинаковых столбцов??
            if (columnNameList.get(col).equals(field)) {
                nameColCount = col;
                Main.isExistField = true;
            }
        }
        if (Main.isExistField) {
            //Если столбец найден
            ArrayList<String> fieldList = columnList.get(nameColCount);
            ArrayList<String> uniqueNameList = new ArrayList<String>();
            //Создаем массив уникальных значений столбца
            uniqueNameList = JTableAPI.getUniqueList(fieldList);
            //System.out.println("Количество уникалных записей: " + uniqueNameList.size());
            LOGGER.log(Level.INFO,"Количество уникалных записей: " + uniqueNameList.size());
            //Создаем разделенные таблицы
            //Инициализируем массив столбцов по размеру общего заголовка
            ArrayList<ArrayList> columnList1 = new ArrayList<ArrayList>();
            for (int i = 0; i < colCount; i++){
               columnList1.add( new ArrayList<String>() );
                }
            //Для каждого значений в списке уникальных полей
            for (int i = 0; i < uniqueNameList.size(); i++){
                //
                for (int row = 0; row < rowCount; row++) {
                    for (int col = 0; col < colCount; col++) {
                        if (uniqueNameList.get(i).equals(fieldList.get(row))){
                            columnList1.get(col).add(columnList.get(col).get(row));
                        }
                        else{
                            break;
                        }
                    }
                }
                LOGGER.log(Level.INFO,"Поле: " + uniqueNameList.get(i));
                JTable tmpTable = new JTable(JTableAPI.buildTableModel_Array(columnNameList, columnList1));
                LOGGER.log(Level.INFO,"Столбцов в таблице: " + tmpTable.getColumnCount() +"; Строк в таблице: "+ tmpTable.getRowCount());
                result.put(uniqueNameList.get(i),tmpTable);
                //Инициализируем массив столбцов по размеру общего заголовка
                columnList1 = new ArrayList<ArrayList>();
                for (int k = 0; k < colCount; k++){
                    columnList1.add( new ArrayList<String>() );
                }
            }
            return result;
        }
        else{
            LOGGER.log(Level.INFO,"Столбец " + field + " не обнаружен");
            return null;
        }
    }

    public static JTable concat (JTable table1, JTable table2){
        if ( table1 == null && table2 == null){
            LOGGER.log(Level.WARNING,"Внимание! Таблицы не созданы! Объединение не возможно");
            return null;
        }
        else{
            if (table1 == null && table2 != null) {
                return table2;
            }
            if (table1 != null && table2 == null) {
                return table1;
            }
            int colCount1 = table1.getColumnCount();
            int colCount2 = table2.getColumnCount();
            int rowCount1 = table1.getRowCount();
            int rowCount2 = table2.getRowCount();
            if (colCount1 != colCount2) {
                //System.out.println("Внимание! Таблицы имеют разное количество столбцов! Объединение не возможно");
                LOGGER.log(Level.WARNING,"Внимание! Таблицы имеют разное количество столбцов! Объединение не возможно");
                return null;
            }
            else{
                //Переводим в массив таблицу table1
                ArrayList<String> columnNameList = new ArrayList<String>();
                ArrayList<ArrayList> columnList1 = new ArrayList<ArrayList>();
                ChangeData.getTableArray(table1, columnNameList, columnList1);
                //Переводим в массив таблицу table2
                columnNameList = new ArrayList<String>();
                ArrayList<ArrayList> columnList2 = new ArrayList<ArrayList>();
                ChangeData.getTableArray(table2, columnNameList, columnList2);
                //Инициализируем массив столбцов по размеру общего заголовка
                ArrayList<ArrayList> resultColumnList = new ArrayList<ArrayList>();
                for (int k = 0; k < colCount1; k++){
                    resultColumnList.add( new ArrayList<String>() );
                }
                //Последовательная запись двух массивов столбцов в один
                int rows = (rowCount1 + rowCount2);
                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < colCount1; col++) {
                        if (row < rowCount1){
                            resultColumnList.get(col).add(columnList1.get(col).get(row));
                        }
                        else{
                            resultColumnList.get(col).add(columnList2.get(col).get(row - rowCount1));
                        }
                    }
                }
                return new JTable(JTableAPI.buildTableModel_Array(columnNameList, resultColumnList));
            }
        }
    }
}


