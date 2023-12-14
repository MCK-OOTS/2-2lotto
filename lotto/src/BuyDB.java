
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BuyDB {
    
    String strDriver = "com.mysql.cj.jdbc.Driver";
    String strURL = "jdbc:mysql://localhost:3306/lotto?characterEncoding=UTF-8&serverTimezone=UTC&useSSL=false";
    String strUser="root";
    String strPwd="root";
    
    Connection DB_con;
    Statement DB_stmt;
    ResultSet DB_rs;
    
    public void dbOpen() throws IOException{
        try{
            Class.forName(strDriver);
            DB_con=DriverManager.getConnection(strURL, strUser, strPwd);
            DB_stmt = DB_con.createStatement();
        }catch(Exception e){
            System.out.println("dbopen SQLEXception" + e.getMessage());
        }
    }
    
    public void dbClose() throws IOException{
        try{
            DB_stmt.close();
            DB_con.close();
        }catch(SQLException e){
            System.err.println("dbcolose SQLEXception" + e.getMessage());
        }
    }   
    
    public void insertWinNum(){
        
    }
}

