/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author cczzs
 */
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class LottoNumCrawler {
    BuyDB BDB = new BuyDB();
    final String url = "https://dhlottery.co.kr/gameResult.do?method=byWin&wiselog=H_C_1_1"; // 동행복권 사이트 주소
    public List<String> getWinningNumbers() throws SQLException {
        
        List<String> numbers = new ArrayList<>();
        
        try {
            Document doc = Jsoup.connect(url).get();

            Elements winningNumbers = doc.select("div.nums div.num.win span.ball_645"); // 사이트에서 번호 크롤링
            for (Element number : winningNumbers) {
                numbers.add(number.text());
            }

            Element bonusNumber = doc.select("div.nums div.num.bonus span.ball_645").first(); // 보너스 번호 크롤링
            if (bonusNumber != null) {
                numbers.add(bonusNumber.text());
            }
            String round = getRound();
            int result = isRound(round);
            // 회차 정보 데이터베이스에 넣기
            if(result == 0){ // 회차가 데이터베이스에 없으면
                String sql = "Insert Into roundinfo Values(?,?,?,?,?,?,?,?)";
                BDB.dbOpen();
                PreparedStatement pstat = BDB.DB_con.prepareStatement(sql);
                pstat.setString(1,round);
                for(int i = 0; i<=6;i++){
                     pstat.setString(i+2,numbers.get(i));
                }
                pstat.executeUpdate();
                pstat.close();
                BDB.dbClose();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
        return numbers; // 리스트 반환
    }
    public int isRound(String round) throws IOException, SQLException{
        // 회차 저장되어 있는지 확인
        int roundNum = Integer.parseInt(round);
        String sql = "Select * from roundinfo where round="+roundNum+";";
        System.out.println(sql);
        BDB.dbOpen();
        BDB.DB_rs = null;
        BDB.DB_rs = BDB.DB_stmt.executeQuery(sql);	
        int result = 0;
        if(BDB.DB_rs.next()){
            result = 1;
        }
        BDB.dbClose();
        return result;
    }
    public String getRound() throws IOException{
        // 회차 값
        String round = null;
        Document doc = Jsoup.connect(url).get();
            
        Element h4Element = doc.select("div.win_result h4 strong").first();
            if (h4Element != null) {
                round = h4Element.text();
                round = round.replace("회", "");
                System.out.println(round);
            }
        return round; // 회차 반환
    }
}
