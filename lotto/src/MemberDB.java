
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


public class MemberDB {
    String strDriver = "com.mysql.cj.jdbc.Driver";
    String strURL = "jdbc:mysql://localhost:3306/lotto?characterEncoding=UTF-8&serverTimezone=UTC&useSSL=false";
    String strUser="root";
    String strPwd="root";
    
    Connection DB_con;
    Statement DB_stmt;
    ResultSet DB_rs;
    
    public void dbOpen() throws IOException{
        try{
            System.out.println("[MemberDB] dbOpen()");
            Class.forName(strDriver);
            DB_con=DriverManager.getConnection(strURL, strUser, strPwd);
            DB_stmt = DB_con.createStatement();
        }catch(Exception e){
            System.out.println("dbopen SQLEXception" + e.getMessage());
        }
    }
    
    public void dbClose() throws IOException{
        try{
            System.out.println("[MemberDB] dbClose()");
            DB_stmt.close();
            DB_con.close();
        }catch(SQLException e){
            System.err.println("dbcolose SQLEXception" + e.getMessage());
        }
    }
    // 로그인
    public boolean login(String id, String password) {
        String sql = "SELECT * FROM member WHERE id = '" + id + "' AND pass = '" + password + "';";
        try {
            dbOpen();
            DB_rs = DB_stmt.executeQuery(sql);
            boolean loginSuccess = DB_rs.next();
            dbClose();
            return loginSuccess;
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }
    // 구입한 번호 가져오기
    public List<String[]> purchasedNumbers(String id) {
        List<String[]> numbersList = new ArrayList<>();
        String sql = "SELECT n1, n2, n3, n4, n5, n6 FROM buyinfo WHERE id = '" + id + "';";
        try {
            dbOpen();
            DB_rs = DB_stmt.executeQuery(sql);
            while (DB_rs.next()) {
                String[] numbers = new String[6]; // 6개 번호
                numbers[0] = DB_rs.getString("n1");
                numbers[1] = DB_rs.getString("n2");
                numbers[2] = DB_rs.getString("n3");
                numbers[3] = DB_rs.getString("n4");
                numbers[4] = DB_rs.getString("n5");
                numbers[5] = DB_rs.getString("n6");
                numbersList.add(numbers);
            }
            dbClose();
        } catch (Exception e) {
            System.out.println(e);
        }
        return numbersList;
    }
}
