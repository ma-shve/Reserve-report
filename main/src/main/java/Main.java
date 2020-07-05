import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

class Main {
    public static boolean isExistField = false;
    //Массив строк - конфигураций
    //cfg.get(0) - путь к файлу конфигурации логера
    //cfg.get(1) - путь к файлу запроса
    //cfg.get(2) - путь к файлу плана
    //cfg.get(3) - путь к файлу резервов
    //cfg.get(4) - лист с таблицей плана
    //cfg.get(5) - лист с плоской таблицей резервов
    //cfg.get(6) - Наименование столбца с отделами из БД
    //cfg.get(7) - Наименование столбца агрегированных по территориальному признаку отделов из БД
    //cfg.get(8) - Целевое расположение готовых отчетов
    public static ArrayList<String> cfg = Config.read();
    static Logger LOGGER;
    static {
        //полный путь до файла с конфигами
        try(FileInputStream ins = new FileInputStream(cfg.get(0))){
            LogManager.getLogManager().readConfiguration(ins);
            LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws IOException {
        /*for (int i = 0; i < cfg.size(); i++) {
            System.out.println(cfg.get(i));
        }*/
        String request = "";
        //Календарь
        LocalDateTime date = LocalDateTime.now();
        String year = String.valueOf(date.getYear());
        String month = "";
        if (date.getMonth().getValue() < 10){
            month = "0"+String.valueOf(date.getMonth().getValue());
        }
        else{
            month = String.valueOf(date.getMonth().getValue());
        }
        String day = "";
        if (date.getDayOfMonth() < 10){
            day = "0"+String.valueOf(date.getDayOfMonth());
        }
        else{
            day = String.valueOf(date.getDayOfMonth());
        }
        String sDate = year + "-" + month + "-" + day;
        sDate = sDate.replace("-","");
        try{
            LOGGER.info("Чтение файла, содержащего SQL запрос");
            List<String> lines = Files.readAllLines(Paths.get(cfg.get(1)), StandardCharsets.UTF_8);
            for(String line: lines){
                request += line + "\n";
            }
        }catch (Exception e){
            LOGGER.log(Level.WARNING,"Внимание! Ошибка при чтении файла." , e);
        }
        JTable table, tablePlan = null;
        //Чтение из Excel плана по отделам
        File file = null;
        try {
            file = new File(cfg.get(2));
            tablePlan = ExcelAPI.read(file, cfg.get(4));
        }catch(Exception e){
            LOGGER.log(Level.WARNING,"Внимание! Файл плановых показателей не обнаружен." , e);
        }
        //Чтение из SQL
        try{
            table = SQLReader.read(request);
            //Добавить пустой столбец "Филиал"
            //Вставить в указанный индекс со сдвигом остальных на +1
            table = JTableAPI.addColumn(table, cfg.get(7),1);
            //Добавить в столбец "Филиал" агрегированные данные из подразделений
            table = JTableAPI.fillColumn(table, cfg.get(6),cfg.get(7));
            //Соединить данные по плану с общей таблицей
            table = JTableAPI.leftJoin(table, tablePlan,cfg.get(7));
            //Путь для записи итогового файла
            String fileName = cfg.get(3) + sDate + ".xlsx";
            String sheetName = cfg.get(5);
            //Раочая книга
            XSSFWorkbook wb = ExcelAPI.create(fileName);
            //Указать тип столбцов до записи в Excel
            HashMap<Integer, String> typeMap = ExcelAPI.setMap("reserve");
            //Запись
            ExcelAPI.writeBook(wb, sheetName, table, typeMap);
            ExcelAPI.setStyle(wb, sheetName, typeMap);
            ExcelAPI.writeToDisk(wb, fileName);
            //Карта имен файлов
            //cfg.get(16) - путь к файлу справочнику коротких наименований структуры предприятия
            //cfg.get(17) - Лист расположения справочника в файле
            LOGGER.log(Level.INFO,"Поиск соответствия имен филиалов и их сокращений в файле: " + cfg.get(16));
            HashMap<String, String> nameMap = ChangeData.getNameMap(cfg.get(16), cfg.get(17));
            //Разделить таблицы на несколько уикальных, по каждому значению поля Филиал
            HashMap<String, JTable> tableMap = JTableAPI.splitByField(table, cfg.get(7));
            //Сколько раз встречается каждое значение в карте имен файлов
            HashMap<String, Integer> nameMapCount = new HashMap<String, Integer>();
            for (Map.Entry entryName: nameMap.entrySet()) {
                String nameValue = (String) entryName.getValue();
                if(!nameMapCount.containsKey(nameValue)){
                    nameMapCount.put(nameValue,1);
                }
                else{
                    nameMapCount.put(nameValue,nameMapCount.get(nameValue) + 1);
                }
            }
            //Объединение нескольких таблиц, относящихся к одному отделу.
            for (Map.Entry entryMapCount: nameMapCount.entrySet()) {
                String countKey = (String) entryMapCount.getKey();
                int contValue = (Integer) entryMapCount.getValue();
                if (contValue > 1) {
                    //Если записей больше одной надо объединять таблицы
                    ArrayList<String> keyList = new ArrayList<String>();
                    ArrayList<JTable> tableList = new ArrayList<JTable>();
                    JTable tmpTable = null;
                    //Записываем список полей к объединению
                    for (Map.Entry entryName: nameMap.entrySet()) {
                        String nameKey = (String) entryName.getKey();
                        String nameValue = (String) entryName.getValue();
                        if (nameValue.equals(countKey)){
                            keyList.add(nameKey);
                        }
                    }
                    //Записываем список таблиц к объеденению
                    for (Map.Entry entryTable: tableMap.entrySet()) {
                        String tableKey = (String) entryTable.getKey();
                        JTable tableValue = (JTable) entryTable.getValue();
                        for(int cnt = 0; cnt < keyList.size(); cnt++){
                            if (tableKey.equals(keyList.get(cnt))){
                                tableList.add(tableValue);
                            }
                        }
                    }
                    //Объединяем таблицы
                    for (int i = 0; i < tableList.size(); i++){
                        tmpTable = JTableAPI.concat(tmpTable,tableList.get(i));
                        }
                    //Удаляем повторяющиеся значения в карте имен
                    Iterator<Map.Entry<String, JTable>> entryIterator = tableMap.entrySet().iterator();
                    while (entryIterator.hasNext()) {
                        Map.Entry<String, JTable> entry = entryIterator.next();
                        for(int cnt = 0; cnt < keyList.size(); cnt++){
                            if (entry.getKey().equals(keyList.get(cnt))) {
                                entryIterator.remove();
                            }
                        }
                    }
                    //Добавляем в карту новую таблицу
                    tableMap.put(keyList.get(0) ,tmpTable);
                }
            }

            for (Map.Entry entryTable: tableMap.entrySet()) {
                for (Map.Entry entryName: nameMap.entrySet()) {
                    String tableKey = (String) entryTable.getKey();
                    JTable tableValue = (JTable) entryTable.getValue();
                    String nameKey = (String) entryName.getKey();
                    String nameValue = (String) entryName.getValue();
                    if (tableKey.equals(nameKey)){
                        //Путь для записи итогового файла
                        fileName = cfg.get(8) + nameValue + sDate + ".xlsx";
                        //Рабочая книга
                        wb = ExcelAPI.create(fileName);
                        //Запись
                        ExcelAPI.writeBook(wb, sheetName, tableValue, typeMap);
                        ExcelAPI.setStyle(wb, sheetName, typeMap);
                        ExcelAPI.writeToDisk(wb, fileName);
                    }
                }
            }
        }catch (Exception e){
            LOGGER.log(Level.WARNING,"Внимание! Ошибка при приобразовании данных." , e);
        }
    }
}



