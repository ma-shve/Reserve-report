import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.FileInputStream;
import java.lang.invoke.MethodHandles;
import java.sql.*;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class SQLReader {
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
    public static JTable read(String request){
        // cfg.get(9) serverName
        // cfg.get(10) dbName
        // cfg.get(11) userName
        // cfg.get(12) pass
        // cfg.get(13) serverConfig
        String connectionUrl = Main.cfg.get(9) + Main.cfg.get(10) + Main.cfg.get(11) + Main.cfg.get(12) + Main.cfg.get(13);
        JTable table = null;
        Driver driver;
        try (Connection connection = DriverManager.getConnection(connectionUrl);) {
            driver = new com.microsoft.sqlserver.jdbc.SQLServerDriver();
            DriverManager.registerDriver(driver);
            LOGGER.log(Level.INFO,"Соединение установлено");
            Statement statement=connection.createStatement();
            LOGGER.log(Level.INFO,"Чтение данных из БД");
            ResultSet resultSet=statement.executeQuery(request);
            LOGGER.log(Level.INFO,"Запись в виртуальную таблицу");
            table = new JTable(JTableAPI.buildTableModel_SQL(resultSet));
            resultSet.close();
            statement.close();
        }
        catch (SQLException e) {
            LOGGER.log(Level.WARNING,"Внимание! При подключении к серверу возникла ошибка!");
            System.exit(1);
        }
        return table;
    }
}
