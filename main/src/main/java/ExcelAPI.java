import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import javax.swing.*;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ExcelAPI{
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

    public static HashMap<Integer, String> setMap (String mapTable) {
        HashMap<Integer, String> colMap = new HashMap<>();
        switch(mapTable){
            case "reserve":
                colMap.put(0 ,"String");        //Подразделение
                colMap.put(1 ,"String");        //Филиал
                colMap.put(2 ,"String");        //Склад
                colMap.put(3 ,"String");        //Обеспечение
                colMap.put(4 ,"String");        //Номер
                colMap.put(5 ,"Date");          //ДатаСоздания
                colMap.put(6 ,"String");        //Партнер
                colMap.put(7 ,"String");        //Менеджер
                colMap.put(8 ,"String");        //Артикул_Товара
                colMap.put(9 ,"String");        //Наименование
                colMap.put(10 ,"String");       //Характеристика
                colMap.put(11 ,"String");       //ВидНоменклатуры
                colMap.put(12 ,"CurDouble");    //СуммаДокумента
                colMap.put(13 ,"Integer");      //Количество
                colMap.put(14 ,"CurDouble");    //СуммаОплаты
                colMap.put(15 ,"PrDouble");     //ПроцентОплат
                colMap.put(16 ,"String");       //Оплачено
                colMap.put(17 ,"CurDouble");    //Цена
                colMap.put(18 ,"Integer");      //ДнейСМоментаСоздания
                colMap.put(19 ,"CurDouble");    //План
        }
        return colMap;
    }

    public static XSSFWorkbook create(String fileName) throws IOException {
        // Создание книги Excel
        XSSFWorkbook book = new XSSFWorkbook();
        FileOutputStream fileOut = new FileOutputStream(fileName);
        // создание страниц
        //cfg.get(5) - лист с плоской таблицей резервов
        XSSFSheet sheet = book.createSheet(Main.cfg.get(5));
        book.write(fileOut);
        fileOut.close();
        LOGGER.log(Level.INFO,"Файл создан путь: " + fileName);
        return book;
    }

    public static JTable read(File file, String sheetName) {
        LOGGER.log(Level.INFO,"Чтение таблицы с диска путь: " + file.getPath());
        ZipSecureFile.setMinInflateRatio(0);
        JTable table = null;
        ArrayList<String> columnNameList = new ArrayList<>();
        ArrayList<ArrayList> columnList = new ArrayList<>();
        try {
            FileInputStream fs = new FileInputStream(file);
            XSSFWorkbook wb = new XSSFWorkbook(file.getCanonicalPath());
            XSSFSheet sheet = wb.getSheet(sheetName);
            XSSFRow sheetRow = sheet.getRow(0);
            XSSFCell sheetCell = null;
            LOGGER.log(Level.INFO,"Столбцов обнаружено: " + sheetRow.getLastCellNum());
            for (int i = 0; i <= sheetRow.getLastCellNum(); i++) {
                sheetCell = sheetRow.getCell(i);
                if (sheetCell != null){
                    columnNameList.add(sheetCell.getStringCellValue());
                    columnList.add( new ArrayList<>() );
                }
            }
            Iterator<Row> iterator = sheet.iterator();
            LOGGER.log(Level.INFO,"Запись в память");
            while (iterator.hasNext()) {
                Row nextRow = iterator.next();
                Iterator<Cell> cellIterator = nextRow.cellIterator();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    if (cell.getRowIndex() > 0){
                        ArrayList<String> list = columnList.get(cell.getColumnIndex());
                        switch (cell.getCellType()) {
                            case STRING:
                                list.add(cell.getStringCellValue());
                                break;
                            case BOOLEAN:
                                list.add(String.valueOf(cell.getBooleanCellValue()));
                                break;
                            case NUMERIC:
                                list.add(String.valueOf(cell.getNumericCellValue()));
                                break;
                        }
                    }
                }
            }
            table = new JTable(JTableAPI.buildTableModel_Array(columnNameList, columnList));
            fs.close();
            wb.close();
            return table;
        }
        catch (IOException e) {
            LOGGER.log(Level.WARNING,"Файл не найден");
            return null;
        }
    }


    public static void setStyle(XSSFWorkbook book,String sheetName,  HashMap<Integer, String> typeMap) {
        LOGGER.log(Level.INFO,"Форматирование таблицы");
        XSSFSheet sheet = book.getSheet(sheetName);
        XSSFCellStyle headerStyle = book.createCellStyle();
        Row row = sheet.getRow(0);
        Cell cell = null;
        int colRow = sheet.getLastRowNum();
        int colCount = row.getLastCellNum();
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, colCount-1));
        sheet.createFreezePane(0,1);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setWrapText(true);
        for (int i = 0; i < colCount; i++) {
            cell = row.getCell(i);
            cell.setCellStyle(headerStyle);
        }

        XSSFCellStyle curCellStyle = book.createCellStyle();
        curCellStyle.setDataFormat(book.createDataFormat().getFormat("_-* #,##0.00[$р.-419]_-;-* #,##0.00[$р.-419]_-;_-* \"-\"??[$р.-419]_-;_-@_-"));
        XSSFCellStyle prCellStyle = book.createCellStyle();
        prCellStyle.setDataFormat(book.createDataFormat().getFormat("0.00%"));
        XSSFCellStyle dateCellStyle = book.createCellStyle();
        dateCellStyle.setDataFormat(book.createDataFormat().getFormat("ДД.ММ.ГГГГ"));
        XSSFCellStyle numCellStyle = book.createCellStyle();
        numCellStyle.setDataFormat(book.createDataFormat().getFormat("# ##0"));
        XSSFCellStyle stringCellStyle = book.createCellStyle();
        stringCellStyle.setDataFormat(book.createDataFormat().getFormat("@"));
        for (int i = 1 ; i <= colRow; i++) {
            row = sheet.getRow(i);
            for (int j = 0; j < colCount; j++) {
                cell = row.getCell(j);
                if (typeMap.containsKey(j)) {
                    switch (typeMap.get(j)) {
                        case "CurDouble":
                            cell.setCellStyle(curCellStyle);
                        break;
                        case "PrDouble":
                            cell.setCellStyle(prCellStyle);
                        break;
                        case "Integer":
                            cell.setCellStyle(numCellStyle);
                            break;
                        case "Date":
                            cell.setCellStyle(dateCellStyle);
                        break;
                        default:
                            cell.setCellStyle(stringCellStyle);
                        break;
                    }
                }
            }
        }
        for (int i = 12 ; i <= colCount; i++) {
            sheet.autoSizeColumn(i);
        }
   }

    public static void writeToDisk (XSSFWorkbook book, String fileName) throws IOException {
        LOGGER.log(Level.INFO,"Запись на диск");
        FileOutputStream fileOut = new FileOutputStream(fileName);
        book.write(fileOut);
        fileOut.close();
        book.close();
        LOGGER.log(Level.INFO,"Файл записан");
    }

    public static void writeBook(XSSFWorkbook book,String sheetName , JTable table,  HashMap<Integer, String> typeMap) throws IOException {
        XSSFSheet sheet = book.getSheet(sheetName);
        int colCount = table.getColumnCount();
        int colRow = table.getRowCount();
        int i;
        Row row;
        Cell cell;
        CellType type;
        //экспорт заголовка таблицы
        row = sheet.createRow(0);
        for (i = 0; i < colCount; i++) {
            cell = row.createCell(i);
            cell.setCellValue(table.getColumnName(i).toString());
        }
        //экспорт данных из JTable в книгу
        LOGGER.log(Level.INFO,"Идет запись в таблицу");
        for (i = 0 ; i < colRow; i++) {
            row = sheet.createRow(i + 1);
            for (int j = 0; j < colCount; j++) {
                cell = row.createCell(j);
                if (typeMap.containsKey(j)) {
                    switch(typeMap.get(j)) {
                        case "Integer":
                            try{
                                String val = table.getValueAt(i, j).toString();
                                if (val.indexOf('.') != -1) {
                                    val = val.substring(0,val.indexOf('.'));
                                    cell.setCellValue(Integer.parseInt(val));
                                }
                                else{
                                    cell.setCellValue(Integer.parseInt(val));
                                }
                            }
                            catch (Exception e){
                                cell.setCellValue(0);
                                }
                            break;
                        case "CurDouble":
                            try{
                                cell.setCellValue(Double.parseDouble(table.getValueAt(i, j).toString()));
                            }
                            catch (Exception e) {
                                cell.setCellValue(0.0);
                            }
                            break;
                        case "PrDouble":
                            try{
                                cell.setCellValue(Double.parseDouble(table.getValueAt(i, j).toString()));
                            }
                            catch (Exception e) {
                                cell.setCellValue(0.0);
                            }
                            break;
                        case "Date":
                            try{
                                cell.setCellValue(table.getValueAt(i, j).toString());
                            }
                            catch (Exception e){
                                cell.setCellValue("01.01.0001");
                            }

                            break;
                        default:
                            try{
                                cell.setCellValue(table.getValueAt(i, j).toString());
                            }
                            catch (Exception e){
                                cell.setCellValue("");
                            }
                            break;
                    }
                }
                else{
                    cell.setCellValue(table.getValueAt(i, j).toString());
                }
            }
        }
        LOGGER.log(Level.INFO,"Строк обработано: " + colRow + "; Столбцов обработано: " + colCount);
    }

}

