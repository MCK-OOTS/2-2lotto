
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;



public class MainFrame extends javax.swing.JFrame {

    private static final int MAX_SELECTED = 6;
    private static JToggleButton[] selectedBtns = new JToggleButton[MAX_SELECTED];
    private static String[] numList = new String[MAX_SELECTED];
    ArrayList<List<JButton>> buyListGroup;
    int price=0;
    private boolean FLAG = false; // 중복확인을 하였는가 안했는가를 확인하는 bool값
    
    BuyDB BDB = new BuyDB();//구매정보 DB
    MemberDB MDB = new MemberDB();
    
    // btn을 List로 관리하기 위해 생성
    // check 속성을 추가함으로 사용된 btn인지 아닌지 체크
    public class Listcheck{
        List<JButton> btnList;
        boolean check = true;
        
        public Listcheck(List<JButton> btnList){
            this.btnList = btnList;
        }
    }
    
    // 구매할 버튼을 선택했을 때 사용
    // 6개가 넘어가면 추가를 하지 않음
    private void btnSelecte(ActionEvent evt){
        JToggleButton button = (JToggleButton) evt.getSource();
        if (button.isSelected()) {
            // 6개가 넘어 가지 않았을 경우에 추가
            for(int i = 0; i<MAX_SELECTED;i++){
                if(selectedBtns[i]==null){
                    selectedBtns[i]=button;
                    numList[i]=button.getText();

                    return;
                }
            }
            // 6개가 넘어 갔을 경우 추가로 넣지 않음
            SwingUtilities.invokeLater(() -> button.setSelected(false));

        }else{
            for(int i = 0; i<MAX_SELECTED;i++){
                if (selectedBtns[i] == button) {
                    selectedBtns[i] = null;
                    return;
                }
            }
        }

    }
    private List<Listcheck> btnListCheck;
    
    //구매버튼 메소드(숫자 선택 되어 있으면 그대로 추가(수동선택), 아니면 임의의 번호선택(자동선택))
    private void inputSelected(){
        for(int i = 0; i<MAX_SELECTED;i++){
            Listcheck btncheck = btnListCheck.get(i);
            HashSet<Integer> usedNumbers = new HashSet<>(); // 숫자 중복 확인용
            if(btncheck.check){ // Listcheck의 boolean 값이 true인 경우
                for (int j = 0; j < MAX_SELECTED; j++) { // Listcheck가 가진 JButton 리스트 순회
                    JButton btn = btncheck.btnList.get(j);
                    JToggleButton tbtn = selectedBtns[j];
                    int num = 0;
                    String strNum = null;
                    if (selectedBtns[j] == null) {
                        do {
                            num = (int) (Math.random() * 45) + 1; // 1 이상 45 이하의 정수 생성
                        } while (usedNumbers.contains(num)); // 이미 사용된 숫자인지 체크
                        usedNumbers.add(num); // 새 숫자를 사용된 숫자의 집합에 추가
                        strNum = Integer.toString(num);
                    } else {
                        strNum = tbtn.getText();
                        usedNumbers.add(Integer.parseInt(strNum)); // 선택된 숫자를 사용된 숫자의 집합에 추가
                    }
                    btn.setText(strNum);
                }
                btncheck.check = false;
                return;
            
            }
            
        }
    }
    
    //초기화 메소드
    private void resetBtns() {
        for (JToggleButton button : selectedBtns) {
            if (button != null) {
                button.setSelected(false); // 버튼을 off 상태로 설정
            }
        }
        Arrays.fill(numList, "0"); // 버튼을 누르면 추가하는 넘버리스트를 0으로 초기화
        Arrays.fill(selectedBtns, null); // 선택되어 있던 버튼을 초기화
    }
    
    // btn을 삭제하는 메서드
    private void deleteSelBtn(int sel){
        Listcheck ListCheck = btnListCheck.get(sel);
        List<JButton> btnList = ListCheck.btnList;
        for(int i = 0; i<btnList.size();i++){
            JButton btn = btnList.get(i);
            btn.setText("");
        }
        ListCheck.check = true; // 체크의 값을 변경
    }
    
    //구매한 번호를 DB테이블에 저장
    public String BDBInsert(List list){
        String str = null;
        int limit = 0; // 구매횟수 제한 
        String SelectSQL = "select max(buyinfo.limit) as 'limit' from lotto.buyinfo where id='"+txtID.getText()+"'";
        try{
            int cnt=0; // 사용자의 총 구매횟수
            BDB.dbOpen();
            PreparedStatement ps = BDB.DB_con.prepareStatement(SelectSQL);
            BDB.DB_rs=ps.executeQuery();
            
            //사용자가 선택한 지금 구매한 횟수를 구하는 반복문
            for(int i=0; i<list.size(); i++){
                str = String.valueOf(list.get(i));
                String[] num = str.split(" ");
                if(str.equals("     ") || str.isEmpty()){}
                else cnt++;
            }
            
            //사용자의 이번회차에 구매한 횟수가 5이상이거나 이전에 구매한 횟수 + 지금 구매하는 수가 5 이상이면 거부
            while(BDB.DB_rs.next()){
                limit = BDB.DB_rs.getInt("limit");
                if(limit >= 5 || limit+cnt > 5){
                    return "fail";
                }   
            }

            BDB.DB_rs.close();
            BDB.dbClose();
        }catch(Exception e){
            System.out.println("sSQLException:"+e.getMessage());
        }
        
        //구매횟수에 이상이 없다면 사용자의 구매 정보를 DB에 추가
        for(int i=0; i<list.size(); i++){
            str = String.valueOf(list.get(i));
            String[] num = str.split(" ");
            if(str.equals("     ") || str.isEmpty()){ // 비어있는 칸은 추가 X
                break;
            }
            String InsertSQL = "insert into buyinfo values(";
            InsertSQL += "'"+txtID.getText()+"',";
            InsertSQL += "'"+num[0]+"',";
            InsertSQL += "'"+num[1]+"',";
            InsertSQL += "'"+num[2]+"',";
            InsertSQL += "'"+num[3]+"',";
            InsertSQL += "'"+num[4]+"',";
            InsertSQL += "'"+num[5]+"',";
            InsertSQL += "0)";
            try{
                BDB.dbOpen();
                BDB.DB_stmt.executeUpdate(InsertSQL);
                BDB.dbClose();
            }catch(Exception e){
                System.out.println("ISQLException:"+e.getMessage());
            }
            //구매횟수 + 1
            String UpdateSQL = "Update buyinfo Set lotto.buyinfo.limit=lotto.buyinfo.limit+1 where id='"+txtID.getText()+"'";
            try{
                BDB.dbOpen();
                BDB.DB_stmt.executeUpdate(UpdateSQL);
                BDB.dbClose();
            }catch(Exception e){
                System.out.println("USQLException:"+e.getMessage());
            }
        }
        
        return "success";    
        
    }
    
        //직전회차 번호를 검색하는 메소드
    private List BeforeRoundUpdate(){
        
        //가장최근에 업데이터된 데이터를 검색
        String SelectSQL = "select * from lotto.roundinfo order by 1 desc limit 1";
        List list = new ArrayList();
        try{
            //setText를 위한 DB연동 구문
            BDB.dbOpen();
            PreparedStatement ps = BDB.DB_con.prepareStatement(SelectSQL);
            BDB.DB_rs=ps.executeQuery();
            while(BDB.DB_rs.next()){
                NumInfo1.setText(String.valueOf(BDB.DB_rs.getInt("n1")));
                NumInfo2.setText(String.valueOf(BDB.DB_rs.getInt("n2")));
                NumInfo3.setText(String.valueOf(BDB.DB_rs.getInt("n3")));     
                NumInfo4.setText(String.valueOf(BDB.DB_rs.getInt("n4")));
                NumInfo5.setText(String.valueOf(BDB.DB_rs.getInt("n5")));  
                NumInfo6.setText(String.valueOf(BDB.DB_rs.getInt("n6")));
                BonusInfo.setText(String.valueOf(BDB.DB_rs.getInt("bonus")));
                
                
            }
            BDB.DB_rs.close();
            BDB.dbClose();
            
            
            BDB.dbOpen();
            ps = BDB.DB_con.prepareStatement(SelectSQL);
            BDB.DB_rs=ps.executeQuery();
            
            
            //직전회차의 당첨번호를 return하기 위해 list에 추가
            while(BDB.DB_rs.next()){
                list.add(BDB.DB_rs.getInt("n1"));
                list.add(BDB.DB_rs.getInt("n2"));
                list.add( BDB.DB_rs.getInt("n3"));
                list.add(BDB.DB_rs.getInt("n4"));
                list.add(BDB.DB_rs.getInt("n5"));
                list.add(BDB.DB_rs.getInt("n6"));
                list.add(BDB.DB_rs.getInt("bonus"));
            }
            BDB.DB_rs.close();
            BDB.dbClose();
        }catch(Exception e){
            System.out.println("BLNI SQLException:"+e.getMessage());
        }
        return list;
    }
    
    
    //이전회차 정보 Update
    private void BeforeRoundInfo() throws SQLException, IOException{
        String SelectSQL = "select * from lotto.buyinfo";
        List list = new ArrayList();
        
        //위의 메소드에서 직전 회차의 당첨번호를 호출해서 받기
        list = BeforeRoundUpdate();
        
        //2등을 가리기 위해 보너스 번호를 따로 추출
        int bonus = Integer.parseInt(String.valueOf(list.remove(list.size() -1)));
        
        int cnt = 0;      // 등수를 가리기 위한 카운트 변수
        int Rank1Cnt = 0; // 1등 수
        int Rank2Cnt = 0;//  2등 수
        int Rank3Cnt = 0;//  3등 수
        int Rank4Cnt = 0;//  4등 수
        int Rank5Cnt = 0;//  5등 수
        try{
            BDB.dbOpen();
            PreparedStatement ps = BDB.DB_con.prepareStatement(SelectSQL);
            BDB.DB_rs=ps.executeQuery();
            
            
            //모든 구매자 정보를 확인하기 위해 While문 사용
            while(BDB.DB_rs.next()){
                //구매자들의 구매정보를 배열로 저장
                List Blist = new ArrayList();
                //
                Blist.add(BDB.DB_rs.getInt("n1"));
                Blist.add(BDB.DB_rs.getInt("n2"));
                Blist.add(BDB.DB_rs.getInt("n3"));
                Blist.add(BDB.DB_rs.getInt("n4"));
                Blist.add(BDB.DB_rs.getInt("n5"));
                Blist.add(BDB.DB_rs.getInt("n6"));
                
                for(int i=0; i<list.size(); i++){
                    for(int j=0; j<Blist.size(); j++){
                        if(list.get(i).equals(Blist.get(j))){
                            cnt++; //구매자의 번호를 당첨번호와 비교하여 일치하면 cnt를 +1
                        }
                    }
                }
                
                switch(cnt){
                    case 6:// 일치하는 번호가 6개면 1등의 당첨자를 +1
                        Rank1Cnt++;
                        break;
                    case 5://일치하는 번호가 5개일 경우 보너스 번호의 포함유무를 확인후 2등과 3등을 구별
                        if(Blist.contains(bonus)){
                            Rank2Cnt++;
                            break;
                        }else{
                            Rank3Cnt++;
                            break;
                        }
                    case 4: //일치하는 번호가 4개면 4등의 당첨자 수를 +1
                        Rank4Cnt++;
                        break;
                    case 3: // 5등 + 1
                        Rank5Cnt++;
                        break;

                }
                cnt=0; // 다음 비교를 위해 0으로 초기화
            }
            
            //등수별로 당첨자수를 setText
            this.Rank1Cnt.setText(String.valueOf(Rank1Cnt));
            this.Rank2Cnt.setText(String.valueOf(Rank2Cnt));
            this.Rank3Cnt.setText(String.valueOf(Rank3Cnt));
            this.Rank4Cnt.setText(String.valueOf(Rank4Cnt));
            this.Rank5Cnt.setText(String.valueOf(Rank5Cnt));
            
        }catch(Exception e){
            System.out.println("BRI SQLException:"+e.getMessage());
        }
        BDB.DB_rs.close();
        BDB.dbClose();
    }
    
    //당첨 많이 된 번호 추출하는 메소드
    private void AllNumInfo(){
        String SelectSQL = "select * from lotto.roundinfo";
        List CntList = new ArrayList();
        for(int i=0; i<=45; i++){
            CntList.add(i,0);
        }
         try{
            BDB.dbOpen();
            PreparedStatement ps = BDB.DB_con.prepareStatement(SelectSQL);
            BDB.DB_rs=ps.executeQuery();
            
            //이전 모든 회차들의 당첨 번호를 Roundlist에 저장
            while(BDB.DB_rs.next()){
                List RoundList = new ArrayList();
                RoundList.add(BDB.DB_rs.getInt("n1"));
                RoundList.add(BDB.DB_rs.getInt("n2"));
                RoundList.add(BDB.DB_rs.getInt("n3"));
                RoundList.add(BDB.DB_rs.getInt("n4"));
                RoundList.add(BDB.DB_rs.getInt("n5"));
                RoundList.add(BDB.DB_rs.getInt("n6"));
                RoundList.add(BDB.DB_rs.getInt("bonus"));
                
                //1~45까지의 번호를 확인해가면서 숫자에 일치하는 RoundList 값이 있다면 Cntlist에 그에 해당하는 값을 +1
                for(int i=1; i<=45; i++){
                    if(RoundList.contains(i)){
                        CntList.set(i, Integer.parseInt(CntList.get(i).toString())+1);
                    }
                }
                
            }
            
            //CntListSort메소드를 호출하면서 매개변수로 CntList전달
            CntListSort(CntList);
        
         }catch(Exception e){
             System.out.println("ANI SQLException:"+e.getMessage());
         }
    }
    
    //가장 많이 나온 숫자 별로 내림차순 정렬 후 setText시키는 메소드
    public void CntListSort(List CntList){
        
        Map<Integer, Integer> map = new HashMap<>();
      
        //map객체의 key, value 추가, 이 때 key는 1~45의 번호 value는 당첨수
        for(int i=1; i<=45; i++){
            map.put( i, Integer.parseInt(CntList.get(i).toString()));
        }
        
        //map객체의 key값을 이용한 SList
        List<Integer> SList = new ArrayList<>(map.keySet());
        
        //SList를 활용해서 내림차순 정렬 
        SList.sort((o1,o2)->map.get(o2).compareTo(map.get(o1)));
        
        //내림차순으로 7가지의 숫자만 setText
        for(int i=0; i<=6; i++){
            switch(i){
                case 0:
                    NumRank1.setText(String.valueOf(SList.get(i)));
                    NumCnt1.setText(String.valueOf(map.get(SList.get(i))));
                    break;
                case 1:
                    NumRank2.setText(String.valueOf(SList.get(i)));
                    NumCnt2.setText(String.valueOf(map.get(SList.get(i))));
                    break;
                case 2:
                    NumRank3.setText(String.valueOf(SList.get(i)));
                    NumCnt3.setText(String.valueOf(map.get(SList.get(i))));
                    break;
                case 3:
                    NumRank4.setText(String.valueOf(SList.get(i)));
                    NumCnt4.setText(String.valueOf(map.get(SList.get(i))));
                    break;
                case 4:
                    NumRank5.setText(String.valueOf(SList.get(i)));
                    NumCnt5.setText(String.valueOf(map.get(SList.get(i))));
                    break;
                case 5:
                    NumRank6.setText(String.valueOf(SList.get(i)));
                    NumCnt6.setText(String.valueOf(map.get(SList.get(i))));
                    break;
                case 6:
                    NumRank7.setText(String.valueOf(SList.get(i)));
                    NumCnt7.setText(String.valueOf(map.get(SList.get(i))));
                    break;
 
            }
            
        }
          
    }
    
    // 크롤링한 리스트를 가지고 와서 번호세팅
    private void setLottoNum(List<String> winningNumbers){
        loto1.setText(winningNumbers.get(0));
        loto2.setText(winningNumbers.get(1));
        loto3.setText(winningNumbers.get(2));
        loto4.setText(winningNumbers.get(3));
        loto5.setText(winningNumbers.get(4));
        loto6.setText(winningNumbers.get(5));
        lotoBonus.setText(winningNumbers.get(6)); // 보너스 번호
    }
    
    private void timeSet(){
        Calendar now = Calendar.getInstance(); // 현재 시간 가져오기
        int today = now.get(Calendar.DAY_OF_WEEK); // 오늘의 요일 가져오기
        int daysUntilSaturday = Calendar.SATURDAY - today; // 토요일까지 남은 일수 계산

         if (daysUntilSaturday < 0) { // 이미 토요일이 지났다면
            daysUntilSaturday += 7;
        } else if (daysUntilSaturday == 0) { // 오늘이 토요일인 경우
            if (now.get(Calendar.HOUR_OF_DAY) > 20 || 
               (now.get(Calendar.HOUR_OF_DAY) == 20 && now.get(Calendar.MINUTE) >= 35)) {
                daysUntilSaturday += 7; // 이미 지정한 시간이 지났으면 다음 주로 설정
            }
        }

        // 가까운 토요일의 날짜와 시간을 설정
        Calendar nextSaturday = (Calendar) now.clone();
        nextSaturday.add(Calendar.DAY_OF_YEAR, daysUntilSaturday);
        nextSaturday.set(Calendar.HOUR_OF_DAY, 20);
        nextSaturday.set(Calendar.MINUTE, 35);
        nextSaturday.set(Calendar.SECOND, 0);
        nextSaturday.set(Calendar.MILLISECOND, 0);

        // 현재부터 토요일까지 남은 시간을 밀리초로 계산
        long millisecondsUntilSaturday = nextSaturday.getTimeInMillis() - now.getTimeInMillis();

        // 남은 시간을 일, 시, 분, 초로 변환
        long secondsUntilSaturday = millisecondsUntilSaturday / 1000;
        long minutesUntilSaturday = secondsUntilSaturday / 60;
        long hoursUntilSaturday = minutesUntilSaturday / 60;
        long days = hoursUntilSaturday / 24;
        long hours = hoursUntilSaturday % 24;
        long minutes = minutesUntilSaturday % 60;
        long seconds = secondsUntilSaturday % 60;

        // 결과 출력
        System.out.println("Time until next Saturday: " + days + " days " + hours + " hours " + minutes + " minutes " + seconds + " seconds");
        String formattedTime = String.format("%02d일%02d시%02d분%02d초", days, hours, minutes, seconds);
        jTextField21.setText(formattedTime);
        jTextField12.setText("추첨까지 남은 시간 : " + formattedTime);
    }
    public MainFrame() throws SQLException, IOException {
        initComponents();
        // 버튼관리를 위해 초기 설정
        List<JButton> btnList1 = new ArrayList<>(Arrays.asList(txtSelNum1_1,txtSelNum1_2,txtSelNum1_3,txtSelNum1_4,txtSelNum1_5,txtSelNum1_6));
        List<JButton> btnList2 = new ArrayList<>(Arrays.asList(txtSelNum2_1,txtSelNum2_2,txtSelNum2_3,txtSelNum2_4,txtSelNum2_5,txtSelNum2_6));
        List<JButton> btnList3 = new ArrayList<>(Arrays.asList(txtSelNum3_1,txtSelNum3_2,txtSelNum3_3,txtSelNum3_4,txtSelNum3_5,txtSelNum3_6));
        List<JButton> btnList4 = new ArrayList<>(Arrays.asList(txtSelNum4_1,txtSelNum4_2,txtSelNum4_3,txtSelNum4_4,txtSelNum4_5,txtSelNum4_6));
        List<JButton> btnList5 = new ArrayList<>(Arrays.asList(txtSelNum5_1,txtSelNum5_2,txtSelNum5_3,txtSelNum5_4,txtSelNum5_5,txtSelNum5_6));
        Listcheck btnListCheck1 = new Listcheck(btnList1);
        Listcheck btnListCheck2 = new Listcheck(btnList2);
        Listcheck btnListCheck3 = new Listcheck(btnList3);
        Listcheck btnListCheck4 = new Listcheck(btnList4);
        Listcheck btnListCheck5 = new Listcheck(btnList5);
        btnListCheck = new ArrayList<>(Arrays.asList(btnListCheck1,btnListCheck2,btnListCheck3,btnListCheck4,btnListCheck5));
        Arrays.fill(numList, "0");
        // 선택한 번호 버튼 그룹
        List<JButton> buyList1 = new ArrayList<>(Arrays.asList(txtBuyNum1_1,txtBuyNum1_2,txtBuyNum1_3,txtBuyNum1_4,txtBuyNum1_5,txtBuyNum1_6));
        List<JButton> buyList2 = new ArrayList<>(Arrays.asList(txtBuyNum2_1,txtBuyNum2_2,txtBuyNum2_3,txtBuyNum2_4,txtBuyNum2_5,txtBuyNum2_6));
        List<JButton> buyList3 = new ArrayList<>(Arrays.asList(txtBuyNum3_1,txtBuyNum3_2,txtBuyNum3_3,txtBuyNum3_4,txtBuyNum3_5,txtBuyNum3_6));
        List<JButton> buyList4 = new ArrayList<>(Arrays.asList(txtBuyNum4_1,txtBuyNum4_2,txtBuyNum4_3,txtBuyNum4_4,txtBuyNum4_5,txtBuyNum4_6));
        List<JButton> buyList5 = new ArrayList<>(Arrays.asList(txtBuyNum5_1,txtBuyNum5_2,txtBuyNum5_3,txtBuyNum5_4,txtBuyNum5_5,txtBuyNum5_6));
        buyListGroup = new ArrayList<>(Arrays.asList(buyList1, buyList2, buyList3, buyList4, buyList5));
        // 크롤링 한 페이지
        LottoNumCrawler crawler = new LottoNumCrawler();
        // 크롤링 한 번호 가져오기
        List<String> winningNumbers = crawler.getWinningNumbers();
        // 가져온 번호 세팅
        setLottoNum(winningNumbers);
        // 시간 세팅
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) { // 무한 루프로 지속적으로 업데이트
                    try {
                        // 1초마다 실행
                        Thread.sleep(1000);

                        // SwingUtilities.invokeLater를 사용하여 EDT에 업데이트를 게시
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                timeSet();
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        // 스레드가 중단되었을 때, 예를 들어 애플리케이션이 닫힐 때
                        // 스레드를 정상적으로 종료하기 위해 break를 사용할 수 있습니다.
                        break;
                    }
                }
            }
        });
        thread.start(); // 스레드 시작
        String round = crawler.getRound();
        jLabel13.setText(round+"회차 당첨번호");
        jTextField11.setText(round+"회차 당첨번호");
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFrame1 = new javax.swing.JFrame();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jPasswordField2 = new javax.swing.JPasswordField();
        jLabel7 = new javax.swing.JLabel();
        jPasswordField3 = new javax.swing.JPasswordField();
        jButton3 = new javax.swing.JButton();
        jTextField2 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jButton4 = new javax.swing.JButton();
        jComboBox2 = new javax.swing.JComboBox<>();
        jComboBox3 = new javax.swing.JComboBox<>();
        jLabel8 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jFrame2 = new javax.swing.JFrame();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel10 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jLabel17 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea3 = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        jTextField11 = new javax.swing.JTextField();
        jTextField12 = new javax.swing.JTextField();
        txtSelNum1_2 = new javax.swing.JButton();
        btn_pur = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        btn_reset = new javax.swing.JButton();
        btn_Del3 = new javax.swing.JButton();
        btn_Del4 = new javax.swing.JButton();
        btn_Del5 = new javax.swing.JButton();
        btn_Del1 = new javax.swing.JButton();
        btn_Del2 = new javax.swing.JButton();
        txtSelNum1_1 = new javax.swing.JButton();
        txtSelNum1_3 = new javax.swing.JButton();
        txtSelNum1_4 = new javax.swing.JButton();
        txtSelNum1_6 = new javax.swing.JButton();
        txtSelNum1_5 = new javax.swing.JButton();
        txtSelNum2_2 = new javax.swing.JButton();
        txtSelNum2_1 = new javax.swing.JButton();
        txtSelNum2_3 = new javax.swing.JButton();
        txtSelNum2_4 = new javax.swing.JButton();
        txtSelNum2_6 = new javax.swing.JButton();
        txtSelNum2_5 = new javax.swing.JButton();
        txtSelNum3_4 = new javax.swing.JButton();
        txtSelNum3_5 = new javax.swing.JButton();
        txtSelNum3_3 = new javax.swing.JButton();
        txtSelNum3_1 = new javax.swing.JButton();
        txtSelNum3_2 = new javax.swing.JButton();
        txtSelNum3_6 = new javax.swing.JButton();
        txtSelNum4_6 = new javax.swing.JButton();
        txtSelNum4_4 = new javax.swing.JButton();
        txtSelNum4_5 = new javax.swing.JButton();
        txtSelNum4_1 = new javax.swing.JButton();
        txtSelNum4_2 = new javax.swing.JButton();
        txtSelNum4_3 = new javax.swing.JButton();
        txtSelNum5_3 = new javax.swing.JButton();
        txtSelNum5_1 = new javax.swing.JButton();
        txtSelNum5_4 = new javax.swing.JButton();
        txtSelNum5_6 = new javax.swing.JButton();
        txtSelNum5_5 = new javax.swing.JButton();
        txtSelNum5_2 = new javax.swing.JButton();
        btn1 = new javax.swing.JToggleButton();
        btn2 = new javax.swing.JToggleButton();
        btn3 = new javax.swing.JToggleButton();
        btn4 = new javax.swing.JToggleButton();
        btn5 = new javax.swing.JToggleButton();
        btn6 = new javax.swing.JToggleButton();
        btn7 = new javax.swing.JToggleButton();
        btn8 = new javax.swing.JToggleButton();
        btn9 = new javax.swing.JToggleButton();
        btn10 = new javax.swing.JToggleButton();
        btn11 = new javax.swing.JToggleButton();
        btn12 = new javax.swing.JToggleButton();
        btn14 = new javax.swing.JToggleButton();
        btn13 = new javax.swing.JToggleButton();
        btn15 = new javax.swing.JToggleButton();
        btn16 = new javax.swing.JToggleButton();
        btn17 = new javax.swing.JToggleButton();
        btn18 = new javax.swing.JToggleButton();
        btn27 = new javax.swing.JToggleButton();
        btn19 = new javax.swing.JToggleButton();
        btn20 = new javax.swing.JToggleButton();
        btn21 = new javax.swing.JToggleButton();
        btn23 = new javax.swing.JToggleButton();
        btn22 = new javax.swing.JToggleButton();
        btn24 = new javax.swing.JToggleButton();
        btn25 = new javax.swing.JToggleButton();
        btn26 = new javax.swing.JToggleButton();
        btn36 = new javax.swing.JToggleButton();
        btn28 = new javax.swing.JToggleButton();
        btn29 = new javax.swing.JToggleButton();
        btn30 = new javax.swing.JToggleButton();
        btn32 = new javax.swing.JToggleButton();
        btn31 = new javax.swing.JToggleButton();
        btn33 = new javax.swing.JToggleButton();
        btn34 = new javax.swing.JToggleButton();
        btn35 = new javax.swing.JToggleButton();
        btn45 = new javax.swing.JToggleButton();
        btn37 = new javax.swing.JToggleButton();
        btn38 = new javax.swing.JToggleButton();
        btn39 = new javax.swing.JToggleButton();
        btn41 = new javax.swing.JToggleButton();
        btn40 = new javax.swing.JToggleButton();
        btn42 = new javax.swing.JToggleButton();
        btn43 = new javax.swing.JToggleButton();
        btn44 = new javax.swing.JToggleButton();
        btn_conf = new javax.swing.JButton();
        lblPrice = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel50 = new javax.swing.JLabel();
        jLabel51 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        jLabel53 = new javax.swing.JLabel();
        jLabel54 = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        jLabel43 = new javax.swing.JLabel();
        jLabel44 = new javax.swing.JLabel();
        jLabel45 = new javax.swing.JLabel();
        jLabel46 = new javax.swing.JLabel();
        jLabel47 = new javax.swing.JLabel();
        NumRank1 = new javax.swing.JLabel();
        NumRank2 = new javax.swing.JLabel();
        NumRank3 = new javax.swing.JLabel();
        NumRank4 = new javax.swing.JLabel();
        NumRank5 = new javax.swing.JLabel();
        NumRank6 = new javax.swing.JLabel();
        NumRank7 = new javax.swing.JLabel();
        jLabel63 = new javax.swing.JLabel();
        jLabel55 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        NumInfo3 = new javax.swing.JLabel();
        NumInfo1 = new javax.swing.JLabel();
        jLabel64 = new javax.swing.JLabel();
        jLabel65 = new javax.swing.JLabel();
        jLabel66 = new javax.swing.JLabel();
        jLabel67 = new javax.swing.JLabel();
        NumInfo2 = new javax.swing.JLabel();
        jLabel68 = new javax.swing.JLabel();
        BonusInfo = new javax.swing.JLabel();
        jLabel69 = new javax.swing.JLabel();
        NumInfo4 = new javax.swing.JLabel();
        jLabel56 = new javax.swing.JLabel();
        NumInfo5 = new javax.swing.JLabel();
        jLabel57 = new javax.swing.JLabel();
        NumInfo6 = new javax.swing.JLabel();
        NumCnt1 = new javax.swing.JLabel();
        jLabel40 = new javax.swing.JLabel();
        NumCnt7 = new javax.swing.JLabel();
        jLabel33 = new javax.swing.JLabel();
        jLabel34 = new javax.swing.JLabel();
        jLabel35 = new javax.swing.JLabel();
        jLabel36 = new javax.swing.JLabel();
        NumCnt6 = new javax.swing.JLabel();
        NumCnt5 = new javax.swing.JLabel();
        NumCnt4 = new javax.swing.JLabel();
        NumCnt3 = new javax.swing.JLabel();
        jLabel37 = new javax.swing.JLabel();
        NumCnt2 = new javax.swing.JLabel();
        jLabel38 = new javax.swing.JLabel();
        jLabel39 = new javax.swing.JLabel();
        jLabel42 = new javax.swing.JLabel();
        Rank1Cnt = new javax.swing.JLabel();
        Rank2Cnt = new javax.swing.JLabel();
        Rank3Cnt = new javax.swing.JLabel();
        Rank4Cnt = new javax.swing.JLabel();
        Rank5Cnt = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        loto1 = new javax.swing.JTextField();
        loto2 = new javax.swing.JTextField();
        loto4 = new javax.swing.JTextField();
        loto3 = new javax.swing.JTextField();
        loto5 = new javax.swing.JTextField();
        loto6 = new javax.swing.JTextField();
        lotoBonus = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jTextField21 = new javax.swing.JTextField();
        btnMyBuyInfo = new javax.swing.JButton();
        jDialog1 = new javax.swing.JDialog();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();
        jPasswordField4 = new javax.swing.JPasswordField();
        jLabel20 = new javax.swing.JLabel();
        jPasswordField5 = new javax.swing.JPasswordField();
        jButton5 = new javax.swing.JButton();
        jTextField6 = new javax.swing.JTextField();
        jTextField7 = new javax.swing.JTextField();
        jButton8 = new javax.swing.JButton();
        jLabel21 = new javax.swing.JLabel();
        jComboBox4 = new javax.swing.JComboBox<>();
        jLabel22 = new javax.swing.JLabel();
        jComboBox5 = new javax.swing.JComboBox<>();
        jButton9 = new javax.swing.JButton();
        jLabel32 = new javax.swing.JLabel();
        jDialog2 = new javax.swing.JDialog();
        lelResult = new javax.swing.JLabel();
        jDialog3 = new javax.swing.JDialog();
        jLabel30 = new javax.swing.JLabel();
        jDialog4 = new javax.swing.JDialog();
        jLabel29 = new javax.swing.JLabel();
        jDialog5 = new javax.swing.JDialog();
        jLabel48 = new javax.swing.JLabel();
        jDialog6 = new javax.swing.JDialog();
        txtBuyNum4_1 = new javax.swing.JButton();
        txtBuyNum5_5 = new javax.swing.JButton();
        txtBuyNum2_6 = new javax.swing.JButton();
        txtBuyNum4_5 = new javax.swing.JButton();
        txtBuyNum5_6 = new javax.swing.JButton();
        txtBuyNum4_6 = new javax.swing.JButton();
        txtBuyNum2_5 = new javax.swing.JButton();
        txtBuyNum4_4 = new javax.swing.JButton();
        txtBuyNum5_4 = new javax.swing.JButton();
        txtBuyNum1_1 = new javax.swing.JButton();
        txtBuyNum2_2 = new javax.swing.JButton();
        txtBuyNum4_3 = new javax.swing.JButton();
        txtBuyNum5_3 = new javax.swing.JButton();
        txtBuyNum1_3 = new javax.swing.JButton();
        txtBuyNum3_4 = new javax.swing.JButton();
        txtBuyNum4_2 = new javax.swing.JButton();
        txtBuyNum5_2 = new javax.swing.JButton();
        txtBuyNum1_4 = new javax.swing.JButton();
        txtBuyNum3_1 = new javax.swing.JButton();
        jLabel71 = new javax.swing.JLabel();
        txtBuyNum1_6 = new javax.swing.JButton();
        txtBuyNum3_6 = new javax.swing.JButton();
        txtBuyNum1_5 = new javax.swing.JButton();
        jLabel72 = new javax.swing.JLabel();
        jLabel73 = new javax.swing.JLabel();
        txtBuyNum1_2 = new javax.swing.JButton();
        jLabel74 = new javax.swing.JLabel();
        txtBuyNum2_1 = new javax.swing.JButton();
        jLabel75 = new javax.swing.JLabel();
        jLabel76 = new javax.swing.JLabel();
        txtBuyNum2_3 = new javax.swing.JButton();
        txtBuyNum3_5 = new javax.swing.JButton();
        txtBuyNum5_1 = new javax.swing.JButton();
        txtBuyNum3_3 = new javax.swing.JButton();
        txtBuyNum2_4 = new javax.swing.JButton();
        txtBuyNum3_2 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        txtID = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jPasswordField1 = new javax.swing.JPasswordField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jLabel31 = new javax.swing.JLabel();

        jLabel3.setText("회원 가입");

        jLabel4.setText("아이디");
        jLabel4.setToolTipText("");

        jLabel5.setText("비밀번호");

        jLabel6.setText("비밀번호 확인");

        jLabel7.setText("성명");

        jButton3.setText("중복확인");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setText("회원가입");

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" }));

        jComboBox3.setMaximumRowCount(31);
        jComboBox3.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31" }));

        jLabel8.setText("생년월일");
        jLabel8.setToolTipText("");

        jTextField3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jFrame1Layout = new javax.swing.GroupLayout(jFrame1.getContentPane());
        jFrame1.getContentPane().setLayout(jFrame1Layout);
        jFrame1Layout.setHorizontalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jFrame1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jFrame1Layout.createSequentialGroup()
                        .addGap(168, 168, 168)
                        .addComponent(jLabel3))
                    .addGroup(jFrame1Layout.createSequentialGroup()
                        .addGroup(jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5)
                            .addComponent(jLabel7)
                            .addComponent(jLabel8))
                        .addGap(18, 18, 18)
                        .addGroup(jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField2)
                            .addComponent(jTextField4)
                            .addComponent(jPasswordField2)
                            .addComponent(jPasswordField3)
                            .addGroup(jFrame1Layout.createSequentialGroup()
                                .addGap(1, 1, 1)
                                .addComponent(jTextField3, javax.swing.GroupLayout.DEFAULT_SIZE, 71, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton3)
                .addGap(6, 6, 6))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jFrame1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton4)
                .addContainerGap())
        );
        jFrame1Layout.setVerticalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jFrame1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jButton3)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9)
                .addGroup(jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jPasswordField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jPasswordField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton4)
                .addContainerGap())
        );

        jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPane1StateChanged(evt);
            }
        });

        jLabel9.setText("당첨조건");

        jTextArea1.setEditable(false);
        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setText("등수                 당첨조건  \t            \t        당첨금액\n\n1둥  | 6개 의 번호가 모두 일치 \t\t|    5,000,000,000원\n2등  | 5개 숫자 일치 + 보너스번호 일치  \t|        50,000,000원\n3등  | 5개 숫자 일치\t \t|          1,000,000원\n4등  | 4개 숫자 일치\t\t|              50,000원\n5등  | 3개 숫자 일치\t\t|                5,000원");
        jTextArea1.setToolTipText("");
        jScrollPane1.setViewportView(jTextArea1);

        jLabel10.setText("구매");

        jTextArea2.setEditable(false);
        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jTextArea2.setText("수동 선택 구매 \t  :  45개의 번호 중 내가 원하는 6개의 번호를 직접 선택하고 구매하는 방법\n\n자동 선택 구매\t  :  45개의 번호 중 임의의 6개 번호가 선택되고 구매하는 방법\n\n반자동 선택 구매 :  45개의 번호 중 1개 이상 또는 5개 이하의 번호를 선택한 후  \n\t     내가 선택하지 않은 번호는 임의의 번호를 선택하는 방법");
        jScrollPane2.setViewportView(jTextArea2);

        jLabel17.setText("구입 시 유의 사항");

        jTextArea3.setEditable(false);
        jTextArea3.setColumns(20);
        jTextArea3.setRows(5);
        jTextArea3.setText("1. 구매시 취소가 불가능합니다.\n2. 한 회차에 최대 5회 구매가 가능합니다.\n\n");
        jScrollPane3.setViewportView(jTextArea3);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel17)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1235, Short.MAX_VALUE)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 546, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane3))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jTabbedPane1.addTab("초보자를 위해", jPanel1);

        jTextField11.setEditable(false);
        jTextField11.setText("제 1회차");

        jTextField12.setEditable(false);
        jTextField12.setText("추첨까지 남은 시간 : DD-HH-MM-SS");
        jTextField12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField12ActionPerformed(evt);
            }
        });

        btn_pur.setText("구매하기");
        btn_pur.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_purActionPerformed(evt);
            }
        });

        jLabel12.setFont(new java.awt.Font("맑은 고딕", 0, 14)); // NOI18N
        jLabel12.setText("선택한 번호");

        btn_reset.setText("초기화");
        btn_reset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_resetActionPerformed(evt);
            }
        });

        btn_Del3.setText("삭제");
        btn_Del3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_Del3ActionPerformed(evt);
            }
        });

        btn_Del4.setText("삭제");
        btn_Del4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_Del4ActionPerformed(evt);
            }
        });

        btn_Del5.setText("삭제");
        btn_Del5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_Del5ActionPerformed(evt);
            }
        });

        btn_Del1.setText("삭제");
        btn_Del1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_Del1ActionPerformed(evt);
            }
        });

        btn_Del2.setText("삭제");
        btn_Del2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_Del2ActionPerformed(evt);
            }
        });

        btn1.setText("1");
        btn1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn2.setText("2");
        btn2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn3.setText("3");
        btn3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn4.setText("4");
        btn4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn5.setText("5");
        btn5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn6.setText("6");
        btn6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn7.setText("7");
        btn7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn8.setText("8");
        btn8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn9.setText("9");
        btn9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn10.setText("10");
        btn10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn11.setText("11");
        btn11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn12.setText("12");
        btn12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn14.setText("14");
        btn14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn13.setText("13");
        btn13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn15.setText("15");
        btn15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn16.setText("16");
        btn16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn17.setText("17");
        btn17.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn18.setText("18");
        btn18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn27.setText("27");
        btn27.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn19.setText("19");
        btn19.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn20.setText("20");
        btn20.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn21.setText("21");
        btn21.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn23.setText("23");
        btn23.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn22.setText("22");
        btn22.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn24.setText("24");
        btn24.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn25.setText("25");
        btn25.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn26.setText("26");
        btn26.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn36.setText("36");
        btn36.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn28.setText("28");
        btn28.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn29.setText("29");
        btn29.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn30.setText("30");
        btn30.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn32.setText("32");
        btn32.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn31.setText("31");
        btn31.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn33.setText("33");
        btn33.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn34.setText("34");
        btn34.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn35.setText("35");
        btn35.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn45.setText("45");
        btn45.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn37.setText("37");
        btn37.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn38.setText("38");
        btn38.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn39.setText("39");
        btn39.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn41.setText("41");
        btn41.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn40.setText("40");
        btn40.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn42.setText("42");
        btn42.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn43.setText("43");
        btn43.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn44.setText("44");
        btn44.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn18ActionPerformed(evt);
            }
        });

        btn_conf.setText("확인(자동선택)");
        btn_conf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_confActionPerformed(evt);
            }
        });

        lblPrice.setFont(new java.awt.Font("맑은 고딕", 0, 24)); // NOI18N
        lblPrice.setText("0원");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jTextField12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(299, 299, 299))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(btn_reset)
                                .addGap(18, 18, 18)
                                .addComponent(btn_conf))
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addComponent(btn28, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn29)
                                    .addGap(10, 10, 10)
                                    .addComponent(btn30, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn31)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn32)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn33, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn34)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn35, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn36, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addComponent(btn19, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn20)
                                    .addGap(10, 10, 10)
                                    .addComponent(btn21, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn22)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn23)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn24, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn25)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn26, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn27, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(btn1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btn10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(btn2, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btn11))
                                    .addGap(12, 12, 12)
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(btn3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btn12, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                            .addComponent(btn13)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(btn14))
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                            .addComponent(btn4, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(btn5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(btn15)
                                        .addComponent(btn6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(btn16))
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                            .addGap(5, 5, 5)
                                            .addComponent(btn7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(btn17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btn8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addGap(6, 6, 6)
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(btn9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btn18, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addComponent(jTextField11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addComponent(btn37, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn38)
                                    .addGap(10, 10, 10)
                                    .addComponent(btn39, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn40)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn41)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn42, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn43)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn44, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btn45, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(147, 147, 147)
                        .addComponent(jLabel12))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(txtSelNum1_1, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(56, 56, 56)
                                .addComponent(txtSelNum1_3, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSelNum1_4, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSelNum1_5, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSelNum1_6, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_Del1, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(txtSelNum2_1, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSelNum2_2, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSelNum2_3, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSelNum2_4, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSelNum2_5, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSelNum2_6, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_Del2, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(1, 1, 1)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addGap(49, 49, 49)
                                                .addComponent(txtSelNum1_2, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addComponent(lblPrice, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btn_pur))
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                .addGroup(jPanel2Layout.createSequentialGroup()
                                                    .addComponent(txtSelNum4_1, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(txtSelNum4_2, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(txtSelNum4_3, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(txtSelNum4_4, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(txtSelNum4_5, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(txtSelNum4_6, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGroup(jPanel2Layout.createSequentialGroup()
                                                    .addComponent(txtSelNum5_1, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(txtSelNum5_2, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(txtSelNum5_3, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(txtSelNum5_4, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(txtSelNum5_5, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(txtSelNum5_6, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(btn_Del4, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btn_Del5, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                            .addComponent(txtSelNum3_1, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtSelNum3_2, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtSelNum3_3, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtSelNum3_4, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtSelNum3_5, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtSelNum3_6, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(btn_Del3, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))))))))
                .addContainerGap(365, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(42, 42, 42)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jTextField11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addGap(9, 9, 9)
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btn1, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btn2, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btn3, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btn4, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btn5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(btn6, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(btn7, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(btn8, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btn9, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(btn12, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btn13, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btn14, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btn15, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btn16, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btn17, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btn18, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(btn10, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btn11, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn19, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn20, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn21, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn22, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn23, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn24, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn25, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn26, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn27, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn28, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn29, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn30, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn31, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn32, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn33, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn34, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn35, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn36, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn37, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn38, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn39, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn40, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn41, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn42, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn43, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn44, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn45, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(10, 10, 10)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_reset)
                            .addComponent(btn_conf))
                        .addContainerGap(318, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtSelNum1_2, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(txtSelNum1_1, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtSelNum1_3, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtSelNum1_4, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtSelNum1_6, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtSelNum1_5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(btn_Del1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(txtSelNum2_2, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtSelNum2_1, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtSelNum2_3, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtSelNum2_4, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtSelNum2_6, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtSelNum2_5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(btn_Del2))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(txtSelNum3_2, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtSelNum3_1, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtSelNum3_3, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtSelNum3_4, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtSelNum3_6, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtSelNum3_5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(4, 4, 4)
                                .addComponent(btn_Del3)))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(txtSelNum4_2, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtSelNum4_1, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtSelNum4_3, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtSelNum4_4, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtSelNum4_6, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtSelNum4_5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(txtSelNum5_2, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtSelNum5_1, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtSelNum5_3, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtSelNum5_4, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtSelNum5_6, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtSelNum5_5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(2, 2, 2)
                                .addComponent(btn_Del4)
                                .addGap(18, 18, 18)
                                .addComponent(btn_Del5)))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btn_pur)
                            .addComponent(lblPrice, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(67, 67, 67))))
        );

        jTabbedPane1.addTab("구매하기", jPanel2);

        jLabel50.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel50.setText("5,000,000,000원");

        jLabel51.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel51.setText("50,000,000원");

        jLabel52.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel52.setText("1,000,000원");

        jLabel53.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel53.setText("50,000원");

        jLabel54.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel54.setText("5,000원");

        jLabel41.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel41.setText("당첨 조건");

        jLabel43.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel43.setText("번호 6개 일치");

        jLabel44.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel44.setText("번호 5개 일치 + 보너스 번호 일치 ");

        jLabel45.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel45.setText("번호 5개 일치");

        jLabel46.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel46.setText("번호 4개 일치");

        jLabel47.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel47.setText("번호 3개일치");

        NumRank1.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        NumRank1.setText("0");

        NumRank2.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        NumRank2.setText("0");

        NumRank3.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        NumRank3.setText("0");

        NumRank4.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        NumRank4.setText("0");

        NumRank5.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        NumRank5.setText("0");

        NumRank6.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        NumRank6.setText("0");

        NumRank7.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        NumRank7.setText("0");

        jLabel63.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel63.setText("순위");

        jLabel55.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel55.setText("1");

        jLabel18.setFont(new java.awt.Font("맑은 고딕", 0, 18)); // NOI18N
        jLabel18.setText("직전회차 당첨번호");

        jLabel11.setFont(new java.awt.Font("맑은 고딕", 0, 18)); // NOI18N
        jLabel11.setText("숫자 별 당첨 정보 Top 7");
        jLabel11.setToolTipText("");

        NumInfo3.setFont(new java.awt.Font("맑은 고딕", 0, 30)); // NOI18N
        NumInfo3.setText("0");

        NumInfo1.setFont(new java.awt.Font("맑은 고딕", 0, 30)); // NOI18N
        NumInfo1.setText("0");

        jLabel64.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel64.setText("7");

        jLabel65.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel65.setText("6");

        jLabel66.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel66.setText("5");

        jLabel67.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel67.setText("4");

        NumInfo2.setFont(new java.awt.Font("맑은 고딕", 0, 30)); // NOI18N
        NumInfo2.setText("0");

        jLabel68.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel68.setText("3");

        BonusInfo.setFont(new java.awt.Font("맑은 고딕", 0, 30)); // NOI18N
        BonusInfo.setText("0");

        jLabel69.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel69.setText("2");

        NumInfo4.setFont(new java.awt.Font("맑은 고딕", 0, 30)); // NOI18N
        NumInfo4.setText("0");

        jLabel56.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel56.setText("당첨 번호");

        NumInfo5.setFont(new java.awt.Font("맑은 고딕", 0, 30)); // NOI18N
        NumInfo5.setText("0");

        jLabel57.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel57.setText("당첨수");

        NumInfo6.setFont(new java.awt.Font("맑은 고딕", 0, 30)); // NOI18N
        NumInfo6.setText("0");

        NumCnt1.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        NumCnt1.setText("0");

        jLabel40.setFont(new java.awt.Font("맑은 고딕", 0, 30)); // NOI18N
        jLabel40.setText("+");

        NumCnt7.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        NumCnt7.setText("0");

        jLabel33.setFont(new java.awt.Font("맑은 고딕", 0, 18)); // NOI18N
        jLabel33.setText("직전회차 당첨 정보");

        jLabel34.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel34.setText("순위");

        jLabel35.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel35.setText("1");

        jLabel36.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel36.setText("2");

        NumCnt6.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        NumCnt6.setText("0");

        NumCnt5.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        NumCnt5.setText("0");

        NumCnt4.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        NumCnt4.setText("0");

        NumCnt3.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        NumCnt3.setText("0");

        jLabel37.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel37.setText("3");

        NumCnt2.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        NumCnt2.setText("0");

        jLabel38.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel38.setText("4");

        jLabel39.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel39.setText("5");

        jLabel42.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel42.setText("당첨자수");

        Rank1Cnt.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        Rank1Cnt.setText("0");

        Rank2Cnt.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        Rank2Cnt.setText("0");

        Rank3Cnt.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        Rank3Cnt.setText("0");

        Rank4Cnt.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        Rank4Cnt.setText("0");

        Rank5Cnt.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        Rank5Cnt.setText("0");

        jLabel49.setFont(new java.awt.Font("맑은 고딕", 0, 16)); // NOI18N
        jLabel49.setText("당첨금");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel34, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(jPanel3Layout.createSequentialGroup()
                                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(jPanel3Layout.createSequentialGroup()
                                                .addComponent(jLabel35, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(64, 64, 64)
                                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                    .addComponent(jLabel42)
                                                    .addComponent(Rank1Cnt, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                            .addGroup(jPanel3Layout.createSequentialGroup()
                                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(jLabel36, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addComponent(jLabel37)
                                                    .addComponent(jLabel38)
                                                    .addComponent(jLabel39))
                                                .addGap(64, 64, 64)
                                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                    .addComponent(Rank5Cnt, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addComponent(Rank4Cnt, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addComponent(Rank3Cnt, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addComponent(Rank2Cnt, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                        .addGap(18, 18, 18)
                                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addComponent(jLabel50, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jLabel49)
                                                .addComponent(jLabel52, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jLabel53, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jLabel54, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                            .addComponent(jLabel51, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addGap(26, 26, 26)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel41)
                                    .addComponent(jLabel43)
                                    .addComponent(jLabel44)
                                    .addComponent(jLabel45)
                                    .addComponent(jLabel46)
                                    .addComponent(jLabel47)))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(NumInfo1, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(NumInfo2, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(NumInfo3, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(NumInfo4, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(NumInfo5, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(NumInfo6, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel40, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(BonusInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 171, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel33, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel11)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel55)
                            .addComponent(jLabel69, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel68, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel67, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel66, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel65, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel64, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel63))
                        .addGap(38, 38, 38)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(NumRank1)
                            .addComponent(NumRank2, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(NumRank3, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(NumRank4, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(NumRank5, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(NumRank6, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(NumRank7, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel56))
                        .addGap(20, 20, 20)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel57)
                            .addComponent(NumCnt1)
                            .addComponent(NumCnt2, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(NumCnt3, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(NumCnt4, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(NumCnt5, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(NumCnt6, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(NumCnt7, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(283, 283, 283))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel18, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(NumInfo1, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(NumInfo2, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(NumInfo3, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(NumInfo4, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(NumInfo5, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(NumInfo6, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(BonusInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel40, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(65, 65, 65)
                .addComponent(jLabel33)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel34)
                    .addComponent(jLabel42)
                    .addComponent(jLabel49)
                    .addComponent(jLabel41))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel35)
                    .addComponent(Rank1Cnt)
                    .addComponent(jLabel50)
                    .addComponent(jLabel43)
                    .addComponent(jLabel63))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel36)
                            .addComponent(Rank2Cnt)
                            .addComponent(jLabel51)
                            .addComponent(jLabel44))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel37)
                            .addComponent(Rank3Cnt, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel52)
                            .addComponent(jLabel45))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel38)
                            .addComponent(Rank4Cnt, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel53)
                            .addComponent(jLabel46))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel39, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(Rank5Cnt, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel54)
                            .addComponent(jLabel47))
                        .addGap(258, 258, 258))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(NumRank1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(NumRank2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(NumRank3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(NumRank4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(NumRank5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(NumRank6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(NumRank7))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel55)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel69)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel68)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel67)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel66)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel65)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel64)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(166, 166, 166)
                .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel57)
                    .addComponent(jLabel56))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(NumCnt1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(NumCnt2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(NumCnt3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(NumCnt4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(NumCnt5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(NumCnt6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(NumCnt7)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("이전 회차 정보", jPanel3);

        jLabel13.setFont(new java.awt.Font("맑은 고딕", 1, 24)); // NOI18N
        jLabel13.setText("1회차 당첨번호");

        loto1.setEditable(false);
        loto1.setFont(new java.awt.Font("맑은 고딕", 1, 48)); // NOI18N
        loto1.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        loto2.setEditable(false);
        loto2.setFont(new java.awt.Font("맑은 고딕", 1, 48)); // NOI18N
        loto2.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        loto4.setEditable(false);
        loto4.setFont(new java.awt.Font("맑은 고딕", 1, 48)); // NOI18N
        loto4.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        loto3.setEditable(false);
        loto3.setFont(new java.awt.Font("맑은 고딕", 1, 48)); // NOI18N
        loto3.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        loto5.setEditable(false);
        loto5.setFont(new java.awt.Font("맑은 고딕", 1, 48)); // NOI18N
        loto5.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        loto6.setEditable(false);
        loto6.setFont(new java.awt.Font("맑은 고딕", 1, 48)); // NOI18N
        loto6.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        lotoBonus.setEditable(false);
        lotoBonus.setFont(new java.awt.Font("맑은 고딕", 1, 48)); // NOI18N
        lotoBonus.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel14.setFont(new java.awt.Font("맑은 고딕", 0, 48)); // NOI18N
        jLabel14.setText("+");

        jTextField21.setEditable(false);
        jTextField21.setFont(new java.awt.Font("맑은 고딕", 0, 24)); // NOI18N
        jTextField21.setText("추첨까지 남은 시간 : DD-HH-MM-SS");
        jTextField21.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField21ActionPerformed(evt);
            }
        });

        btnMyBuyInfo.setFont(new java.awt.Font("맑은 고딕 Semilight", 0, 24)); // NOI18N
        btnMyBuyInfo.setText("나의 구매 정보 보기");
        btnMyBuyInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMyBuyInfoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(344, 344, 344)
                .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 189, Short.MAX_VALUE)
                .addComponent(jTextField21, javax.swing.GroupLayout.PREFERRED_SIZE, 429, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(102, 102, 102))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnMyBuyInfo)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(loto1, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(loto2, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(loto3, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(loto4, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(loto5, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(loto6, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(31, 31, 31)
                .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lotoBonus, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField21, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(loto1, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(loto2, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(loto4, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(loto3, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(loto5, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(loto6, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addComponent(lotoBonus, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(69, 69, 69)
                .addComponent(btnMyBuyInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(147, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("추첨", jPanel4);

        javax.swing.GroupLayout jFrame2Layout = new javax.swing.GroupLayout(jFrame2.getContentPane());
        jFrame2.getContentPane().setLayout(jFrame2Layout);
        jFrame2Layout.setHorizontalGroup(
            jFrame2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
        jFrame2Layout.setVerticalGroup(
            jFrame2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jFrame2Layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 469, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 71, Short.MAX_VALUE))
        );

        jTabbedPane1.getAccessibleContext().setAccessibleName("lotto");

        jDialog1.setModal(true);

        jLabel15.setText("비밀번호");

        jLabel16.setText("생년월일");
        jLabel16.setToolTipText("");

        jLabel19.setText("비밀번호 확인");

        jLabel20.setText("성명");

        jButton5.setText("중복확인");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jButton8.setText("회원가입");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        jLabel21.setText("회원 가입");

        jComboBox4.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" }));

        jLabel22.setText("아이디");
        jLabel22.setToolTipText("");

        jComboBox5.setMaximumRowCount(31);
        jComboBox5.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31" }));

        jButton9.setText("취소");
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jDialog1Layout = new javax.swing.GroupLayout(jDialog1.getContentPane());
        jDialog1.getContentPane().setLayout(jDialog1Layout);
        jDialog1Layout.setHorizontalGroup(
            jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jDialog1Layout.createSequentialGroup()
                        .addGroup(jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jDialog1Layout.createSequentialGroup()
                                .addGap(168, 168, 168)
                                .addComponent(jLabel21))
                            .addGroup(jDialog1Layout.createSequentialGroup()
                                .addGroup(jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel19)
                                    .addComponent(jLabel22)
                                    .addComponent(jLabel15)
                                    .addComponent(jLabel20)
                                    .addComponent(jLabel16))
                                .addGap(18, 18, 18)
                                .addGroup(jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextField6)
                                    .addComponent(jTextField7)
                                    .addComponent(jPasswordField4)
                                    .addComponent(jPasswordField5)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialog1Layout.createSequentialGroup()
                                        .addGap(1, 1, 1)
                                        .addComponent(jTextField5)
                                        .addGap(18, 18, 18)
                                        .addComponent(jComboBox4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(jComboBox5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton5)
                        .addGap(6, 6, 6))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialog1Layout.createSequentialGroup()
                        .addComponent(jButton9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel32, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(69, 69, 69)
                        .addComponent(jButton8)
                        .addContainerGap())))
        );
        jDialog1Layout.setVerticalGroup(
            jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel21)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(jButton5)
                    .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9)
                .addGroup(jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(jPasswordField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19)
                    .addComponent(jPasswordField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(jComboBox4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 70, Short.MAX_VALUE)
                .addGroup(jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton8)
                    .addComponent(jButton9)
                    .addComponent(jLabel32, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        lelResult.setText("구매성공!");

        javax.swing.GroupLayout jDialog2Layout = new javax.swing.GroupLayout(jDialog2.getContentPane());
        jDialog2.getContentPane().setLayout(jDialog2Layout);
        jDialog2Layout.setHorizontalGroup(
            jDialog2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog2Layout.createSequentialGroup()
                .addGap(54, 54, 54)
                .addComponent(lelResult)
                .addContainerGap(62, Short.MAX_VALUE))
        );
        jDialog2Layout.setVerticalGroup(
            jDialog2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog2Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(lelResult)
                .addContainerGap(34, Short.MAX_VALUE))
        );

        jLabel30.setText("구매할 숫자의 조합을 선택해주세요.");

        javax.swing.GroupLayout jDialog3Layout = new javax.swing.GroupLayout(jDialog3.getContentPane());
        jDialog3.getContentPane().setLayout(jDialog3Layout);
        jDialog3Layout.setHorizontalGroup(
            jDialog3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialog3Layout.createSequentialGroup()
                .addContainerGap(58, Short.MAX_VALUE)
                .addComponent(jLabel30)
                .addGap(45, 45, 45))
        );
        jDialog3Layout.setVerticalGroup(
            jDialog3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog3Layout.createSequentialGroup()
                .addGap(63, 63, 63)
                .addComponent(jLabel30)
                .addContainerGap(71, Short.MAX_VALUE))
        );

        jLabel29.setFont(new java.awt.Font("맑은 고딕", 0, 18)); // NOI18N
        jLabel29.setText("한 회차의 최대 구매횟수는 5회 입니다.\n");

        javax.swing.GroupLayout jDialog4Layout = new javax.swing.GroupLayout(jDialog4.getContentPane());
        jDialog4.getContentPane().setLayout(jDialog4Layout);
        jDialog4Layout.setHorizontalGroup(
            jDialog4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog4Layout.createSequentialGroup()
                .addGap(65, 65, 65)
                .addComponent(jLabel29, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(48, Short.MAX_VALUE))
        );
        jDialog4Layout.setVerticalGroup(
            jDialog4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialog4Layout.createSequentialGroup()
                .addGap(53, 53, 53)
                .addComponent(jLabel29, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(35, Short.MAX_VALUE))
        );

        jDialog5.setModal(true);

        jLabel48.setFont(new java.awt.Font("맑은 고딕", 0, 24)); // NOI18N
        jLabel48.setText("동행복권 사이트의 점검시간 입니다.");

        javax.swing.GroupLayout jDialog5Layout = new javax.swing.GroupLayout(jDialog5.getContentPane());
        jDialog5.getContentPane().setLayout(jDialog5Layout);
        jDialog5Layout.setHorizontalGroup(
            jDialog5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel48, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        jDialog5Layout.setVerticalGroup(
            jDialog5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog5Layout.createSequentialGroup()
                .addGap(91, 91, 91)
                .addComponent(jLabel48, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(136, Short.MAX_VALUE))
        );

        jLabel71.setFont(new java.awt.Font("맑은 고딕", 0, 18)); // NOI18N
        jLabel71.setText("내가 구매한 번호");

        jLabel72.setText("1");

        jLabel73.setText("2");

        jLabel74.setText("3");
        jLabel74.setToolTipText("");

        jLabel75.setText("4");

        jLabel76.setText("5");

        txtBuyNum5_1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtBuyNum5_1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jDialog6Layout = new javax.swing.GroupLayout(jDialog6.getContentPane());
        jDialog6.getContentPane().setLayout(jDialog6Layout);
        jDialog6Layout.setHorizontalGroup(
            jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog6Layout.createSequentialGroup()
                .addGap(79, 79, 79)
                .addGroup(jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jDialog6Layout.createSequentialGroup()
                            .addGap(117, 117, 117)
                            .addComponent(jLabel71))
                        .addGroup(jDialog6Layout.createSequentialGroup()
                            .addGroup(jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jLabel75)
                                .addComponent(jLabel74))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jDialog6Layout.createSequentialGroup()
                                    .addComponent(txtBuyNum3_1, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum3_2, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum3_3, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum3_4, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum3_5, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum3_6, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jDialog6Layout.createSequentialGroup()
                                    .addComponent(txtBuyNum4_1, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum4_2, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum4_3, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum4_4, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum4_5, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum4_6, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGroup(jDialog6Layout.createSequentialGroup()
                            .addGroup(jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel72)
                                .addComponent(jLabel73))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jDialog6Layout.createSequentialGroup()
                                    .addComponent(txtBuyNum1_1, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum1_2, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum1_3, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum1_4, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum1_5, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum1_6, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jDialog6Layout.createSequentialGroup()
                                    .addComponent(txtBuyNum2_1, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum2_2, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum2_3, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum2_4, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum2_5, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtBuyNum2_6, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(jDialog6Layout.createSequentialGroup()
                        .addComponent(jLabel76)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtBuyNum5_1, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtBuyNum5_2, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtBuyNum5_3, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtBuyNum5_4, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtBuyNum5_5, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtBuyNum5_6, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(108, Short.MAX_VALUE))
        );
        jDialog6Layout.setVerticalGroup(
            jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialog6Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel71, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialog6Layout.createSequentialGroup()
                        .addGroup(jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(txtBuyNum1_1, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(txtBuyNum1_2, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtBuyNum1_3, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtBuyNum1_4, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtBuyNum1_6, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtBuyNum1_5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(jLabel72, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtBuyNum2_2, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtBuyNum2_1, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtBuyNum2_3, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtBuyNum2_4, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtBuyNum2_6, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtBuyNum2_5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jLabel73, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtBuyNum3_2, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtBuyNum3_1, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtBuyNum3_3, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtBuyNum3_4, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtBuyNum3_6, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtBuyNum3_5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel74, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGroup(jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jDialog6Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtBuyNum4_2, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtBuyNum4_1, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtBuyNum4_3, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtBuyNum4_4, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtBuyNum4_6, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtBuyNum4_5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jDialog6Layout.createSequentialGroup()
                        .addGap(16, 16, 16)
                        .addComponent(jLabel75)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jDialog6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtBuyNum5_2, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtBuyNum5_1, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtBuyNum5_3, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtBuyNum5_4, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtBuyNum5_5, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jDialog6Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel76))
                    .addComponent(txtBuyNum5_6, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(236, 236, 236))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("아이디");

        jLabel2.setText("패스워드");

        jPasswordField1.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jPasswordField1FocusGained(evt);
            }
        });

        jButton1.setText("로그인");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("회원가입");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(105, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jButton1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton2))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel2)
                                    .addComponent(jLabel1))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtID, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jPasswordField1))))
                        .addGap(91, 91, 91))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel31, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(43, 43, 43)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(41, 41, 41)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 32, Short.MAX_VALUE)
                .addComponent(jLabel31, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed

    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // 회원가입 창 띄우기
        jDialog1.setLocation(300,300);
        jDialog1.setSize(450,350);
        jDialog1.show();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        // 회원가입 버튼을 눌렀을 때 작동
        // 회원 정보의 중복을 확인하고
        // 아이디 중복체크를 설정함으로 회원가입이 완료되기 전에 처리를 함
        String sql = "";
        try{
            sql = "select * from member where id = '";
            sql += jTextField6.getText()+"';";
            MDB.dbOpen();
            MDB.DB_rs = null;
            MDB.DB_rs = MDB.DB_stmt.executeQuery(sql);	
            int result = 0;
            if(MDB.DB_rs.next()){	// 이미 등록된 경우
                // 사용자가 등록되어 있음 메시지 출력
                jLabel32.setText("이미 등록된 아이디");
            	jTextField6.setText("");
                FLAG = false; // FLAG는 아이디 중복체크를 하였는가를 체크
            }else{
                FLAG = true;
                jLabel32.setText("중복확인 완료");
            }
            MDB.dbClose();
        }catch(Exception e){
            System.out.println(e);
        }
        
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        // 회원가입 창 끄기
        jDialog1.dispose();
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        String sql = "select * from member where id = '"+txtID.getText()+
							"' and pass = '"+jPasswordField1.getText()+"';";
        System.out.println("[MainFrame] login button : "+sql);
        try{
            if(MDB.login(txtID.getText(),jPasswordField1.getText())){ // 아이디가 저장되어 있으면
                jFrame2.setDefaultCloseOperation(jFrame2.EXIT_ON_CLOSE);
                jFrame2.setLocation(300,300);
                jFrame2.setSize(1000,500);
                jFrame2.setVisible(true);
                this.dispose();
                List<String[]> numbersList = MDB.purchasedNumbers(txtID.getText());
                for (int i = 0; i < numbersList.size(); i++) {
                    String[] numbers = numbersList.get(i); // 선택한 로또 번호 가져오기
                    List<JButton> buttonList = buyListGroup.get(i); // 버튼 리스트 가져오기
                    for (int j = 0; j < buttonList.size(); j++) {
                        JButton button = buttonList.get(j); // 이에서 가져온 버튼 리스트에서 버튼 가져오기
                        button.setText(numbers[j]); // 버튼에 가져온 로또 번호 세팅
                    }
                }
            }
            else{
                // 아이디 비밀번호 확인 메시지
                jLabel31.setText("로그인 실패");
            }
        }catch(Exception e){
            System.out.println(e);
        }
        
        
    }//GEN-LAST:event_jButton1ActionPerformed

    private void btn18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn18ActionPerformed
        // 버튼을 클릭하면 EVT를 전달
        btnSelecte(evt);
    }//GEN-LAST:event_btn18ActionPerformed

    private void btn_confActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_confActionPerformed
        price++;
        lblPrice.setText(String.valueOf(price*5000)+"원");
        inputSelected();
        resetBtns();
    }//GEN-LAST:event_btn_confActionPerformed

    private void btn_resetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_resetActionPerformed
        // 선택한 버튼 리셋
        resetBtns();
    }//GEN-LAST:event_btn_resetActionPerformed

    private void btn_Del1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_Del1ActionPerformed
        //삭제버튼
        price--;
        lblPrice.setText(String.valueOf(price*5000)+"원");
        deleteSelBtn(0);
    }//GEN-LAST:event_btn_Del1ActionPerformed

    private void btn_Del2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_Del2ActionPerformed
        //삭제버튼
        price--;
        lblPrice.setText(String.valueOf(price*5000)+"원");
        deleteSelBtn(1);
    }//GEN-LAST:event_btn_Del2ActionPerformed

    private void btn_Del3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_Del3ActionPerformed
        //삭제버튼
        price--;
        lblPrice.setText(String.valueOf(price*5000)+"원");
        deleteSelBtn(2);
    }//GEN-LAST:event_btn_Del3ActionPerformed

    private void btn_Del4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_Del4ActionPerformed
        //삭제버튼
        price--;
        lblPrice.setText(String.valueOf(price*5000)+"원");
        deleteSelBtn(3);
    }//GEN-LAST:event_btn_Del4ActionPerformed

    private void btn_Del5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_Del5ActionPerformed
        //삭제버튼
        price--;
        lblPrice.setText(String.valueOf(price*5000)+"원");
        deleteSelBtn(4);
    }//GEN-LAST:event_btn_Del5ActionPerformed

    private void jTextField3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField3ActionPerformed

    private void btn_purActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_purActionPerformed
        //구매버튼
        
        //선택된 번호가 없다면 
        if(txtSelNum1_1.getText().isEmpty()){
            jDialog3.setLocation(1000,500);
            jDialog3.setSize(300,150);
            jDialog3.show();
        
        //각 TextField의 값을 문자열로 변환 후 list에 저장
        }else{
            List list = new ArrayList();
            String str = txtSelNum1_1.getText()+" "+txtSelNum1_2.getText()+" "+txtSelNum1_3.getText()+" "+txtSelNum1_4.getText()+" "+txtSelNum1_5.getText()+" "+txtSelNum1_6.getText();
            list.add(str);
            str = txtSelNum2_1.getText()+" "+txtSelNum2_2.getText()+" "+txtSelNum2_3.getText()+" "+txtSelNum2_4.getText()+" "+txtSelNum2_5.getText()+" "+txtSelNum2_6.getText();
            list.add(str);
            str = txtSelNum3_1.getText()+" "+txtSelNum3_2.getText()+" "+txtSelNum3_3.getText()+" "+txtSelNum3_4.getText()+" "+txtSelNum3_5.getText()+" "+txtSelNum3_6.getText();
            list.add(str);
             str = txtSelNum4_1.getText()+" "+txtSelNum4_2.getText()+" "+txtSelNum4_3.getText()+" "+txtSelNum4_4.getText()+" "+txtSelNum4_5.getText()+" "+txtSelNum4_6.getText();
            list.add(str);
             str = txtSelNum5_1.getText()+" "+txtSelNum5_2.getText()+" "+txtSelNum5_3.getText()+" "+txtSelNum5_4.getText()+" "+txtSelNum5_5.getText()+" "+txtSelNum5_6.getText();
            list.add(str);
            
            //DB추가 메소드호출하면서 매개변수로 list(구매정보)전달
            String result = BDBInsert(list);
            
            //실행결과 보여주기 
            if(result.equals("success")){
                jDialog2.setLocation(1000,500);
                jDialog2.setSize(200,150);
                jDialog2.show();
            }else if(result.equals("fail")){
               jDialog4.setLocation(1000,500);
                jDialog4.setSize(500,200);
                jDialog4.show();
            }
        }
        
    }//GEN-LAST:event_btn_purActionPerformed

    private void jTextField12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField12ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField12ActionPerformed

    private void jTextField21ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField21ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField21ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        if(FLAG){
            String sql;
            if(jPasswordField4.getText().equals(jPasswordField5.getText())){ // 비밀번호와 비밀번호 확인의 값이 일치하였을 경우
                try{
                    sql = "Insert Into member Values(?,?,?,?)";
                    MDB.dbOpen();
                    PreparedStatement pstat = MDB.DB_con.prepareStatement(sql);
                    pstat.setString(1,jTextField6.getText());
                    pstat.setString(2,jPasswordField4.getText());
                    pstat.setString(3, jTextField7.getText());
                    
                    String dateStr = jTextField5.getText()+"-"+jComboBox4.getSelectedItem()+"-"+jComboBox5.getSelectedItem(); // 사용자가 입력한 날짜 포맷에 맞게 저장
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                    // 날짜를 데이터베이스에 저장하면 하루 일찍(ex : 11-29 -> 11-28)저장되는걸 방지
                    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                    
                    //사이트 점검 중일 때
                    if(dateStr.isEmpty() || dateStr == null){
                        jDialog5.setLocation(1000,500);
                        jDialog5.setSize(500,200);
                        jDialog5.show();
                    }
                        Date date = formatter.parse(dateStr);
                    
                    // sql에 맞게 조정
                    java.sql.Date sqlDate = new java.sql.Date(date.getTime());
                    
                    pstat.setDate(4, sqlDate);
                    
                    pstat.executeUpdate();
                    
                    MDB.dbClose();
                }catch(Exception e){
                    System.out.println(e);
                }   
            }else{
                // 비밀번호 확인 메시지 출력
                jLabel32.setText("비밀번호 확인");
            }
        }else{
            // 중복확인 버튼 클릭 메시지 출력
            jLabel32.setText("중복확인 클릭");
        }
        // 회원가입 창 끄기
        jDialog1.dispose();
        // 로그인 창에 메시지 출력
        jLabel31.setText("회원가입 완료!");
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jPasswordField1FocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jPasswordField1FocusGained
        // Foucs를 얻을 시 필드 초기화
        jPasswordField1.setText("");
    }//GEN-LAST:event_jPasswordField1FocusGained

    private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane1StateChanged
        //tab 이동 이벤트
        List list = new ArrayList();
        
        list = BeforeRoundUpdate();
        try {
            BeforeRoundInfo();
        } catch (SQLException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        AllNumInfo();
    }//GEN-LAST:event_jTabbedPane1StateChanged

    private void txtBuyNum5_1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtBuyNum5_1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtBuyNum5_1ActionPerformed

    private void btnMyBuyInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMyBuyInfoActionPerformed
            jDialog6.setLocation(1000,500);
            jDialog6.setSize(450,300);
            jDialog6.show();
    }//GEN-LAST:event_btnMyBuyInfoActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new MainFrame().setVisible(true);
                } catch (SQLException ex) {
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel BonusInfo;
    private javax.swing.JLabel NumCnt1;
    private javax.swing.JLabel NumCnt2;
    private javax.swing.JLabel NumCnt3;
    private javax.swing.JLabel NumCnt4;
    private javax.swing.JLabel NumCnt5;
    private javax.swing.JLabel NumCnt6;
    private javax.swing.JLabel NumCnt7;
    private javax.swing.JLabel NumInfo1;
    private javax.swing.JLabel NumInfo2;
    private javax.swing.JLabel NumInfo3;
    private javax.swing.JLabel NumInfo4;
    private javax.swing.JLabel NumInfo5;
    private javax.swing.JLabel NumInfo6;
    private javax.swing.JLabel NumRank1;
    private javax.swing.JLabel NumRank2;
    private javax.swing.JLabel NumRank3;
    private javax.swing.JLabel NumRank4;
    private javax.swing.JLabel NumRank5;
    private javax.swing.JLabel NumRank6;
    private javax.swing.JLabel NumRank7;
    private javax.swing.JLabel Rank1Cnt;
    private javax.swing.JLabel Rank2Cnt;
    private javax.swing.JLabel Rank3Cnt;
    private javax.swing.JLabel Rank4Cnt;
    private javax.swing.JLabel Rank5Cnt;
    private javax.swing.JToggleButton btn1;
    private javax.swing.JToggleButton btn10;
    private javax.swing.JToggleButton btn11;
    private javax.swing.JToggleButton btn12;
    private javax.swing.JToggleButton btn13;
    private javax.swing.JToggleButton btn14;
    private javax.swing.JToggleButton btn15;
    private javax.swing.JToggleButton btn16;
    private javax.swing.JToggleButton btn17;
    private javax.swing.JToggleButton btn18;
    private javax.swing.JToggleButton btn19;
    private javax.swing.JToggleButton btn2;
    private javax.swing.JToggleButton btn20;
    private javax.swing.JToggleButton btn21;
    private javax.swing.JToggleButton btn22;
    private javax.swing.JToggleButton btn23;
    private javax.swing.JToggleButton btn24;
    private javax.swing.JToggleButton btn25;
    private javax.swing.JToggleButton btn26;
    private javax.swing.JToggleButton btn27;
    private javax.swing.JToggleButton btn28;
    private javax.swing.JToggleButton btn29;
    private javax.swing.JToggleButton btn3;
    private javax.swing.JToggleButton btn30;
    private javax.swing.JToggleButton btn31;
    private javax.swing.JToggleButton btn32;
    private javax.swing.JToggleButton btn33;
    private javax.swing.JToggleButton btn34;
    private javax.swing.JToggleButton btn35;
    private javax.swing.JToggleButton btn36;
    private javax.swing.JToggleButton btn37;
    private javax.swing.JToggleButton btn38;
    private javax.swing.JToggleButton btn39;
    private javax.swing.JToggleButton btn4;
    private javax.swing.JToggleButton btn40;
    private javax.swing.JToggleButton btn41;
    private javax.swing.JToggleButton btn42;
    private javax.swing.JToggleButton btn43;
    private javax.swing.JToggleButton btn44;
    private javax.swing.JToggleButton btn45;
    private javax.swing.JToggleButton btn5;
    private javax.swing.JToggleButton btn6;
    private javax.swing.JToggleButton btn7;
    private javax.swing.JToggleButton btn8;
    private javax.swing.JToggleButton btn9;
    private javax.swing.JButton btnMyBuyInfo;
    private javax.swing.JButton btn_Del1;
    private javax.swing.JButton btn_Del2;
    private javax.swing.JButton btn_Del3;
    private javax.swing.JButton btn_Del4;
    private javax.swing.JButton btn_Del5;
    private javax.swing.JButton btn_conf;
    private javax.swing.JButton btn_pur;
    private javax.swing.JButton btn_reset;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JComboBox<String> jComboBox3;
    private javax.swing.JComboBox<String> jComboBox4;
    private javax.swing.JComboBox<String> jComboBox5;
    private javax.swing.JDialog jDialog1;
    private javax.swing.JDialog jDialog2;
    private javax.swing.JDialog jDialog3;
    private javax.swing.JDialog jDialog4;
    private javax.swing.JDialog jDialog5;
    private javax.swing.JDialog jDialog6;
    private javax.swing.JFrame jFrame1;
    private javax.swing.JFrame jFrame2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel63;
    private javax.swing.JLabel jLabel64;
    private javax.swing.JLabel jLabel65;
    private javax.swing.JLabel jLabel66;
    private javax.swing.JLabel jLabel67;
    private javax.swing.JLabel jLabel68;
    private javax.swing.JLabel jLabel69;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel71;
    private javax.swing.JLabel jLabel72;
    private javax.swing.JLabel jLabel73;
    private javax.swing.JLabel jLabel74;
    private javax.swing.JLabel jLabel75;
    private javax.swing.JLabel jLabel76;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JPasswordField jPasswordField2;
    private javax.swing.JPasswordField jPasswordField3;
    private javax.swing.JPasswordField jPasswordField4;
    private javax.swing.JPasswordField jPasswordField5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextArea jTextArea3;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField12;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField21;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JLabel lblPrice;
    private javax.swing.JLabel lelResult;
    private javax.swing.JTextField loto1;
    private javax.swing.JTextField loto2;
    private javax.swing.JTextField loto3;
    private javax.swing.JTextField loto4;
    private javax.swing.JTextField loto5;
    private javax.swing.JTextField loto6;
    private javax.swing.JTextField lotoBonus;
    private javax.swing.JButton txtBuyNum1_1;
    private javax.swing.JButton txtBuyNum1_2;
    private javax.swing.JButton txtBuyNum1_3;
    private javax.swing.JButton txtBuyNum1_4;
    private javax.swing.JButton txtBuyNum1_5;
    private javax.swing.JButton txtBuyNum1_6;
    private javax.swing.JButton txtBuyNum2_1;
    private javax.swing.JButton txtBuyNum2_2;
    private javax.swing.JButton txtBuyNum2_3;
    private javax.swing.JButton txtBuyNum2_4;
    private javax.swing.JButton txtBuyNum2_5;
    private javax.swing.JButton txtBuyNum2_6;
    private javax.swing.JButton txtBuyNum3_1;
    private javax.swing.JButton txtBuyNum3_2;
    private javax.swing.JButton txtBuyNum3_3;
    private javax.swing.JButton txtBuyNum3_4;
    private javax.swing.JButton txtBuyNum3_5;
    private javax.swing.JButton txtBuyNum3_6;
    private javax.swing.JButton txtBuyNum4_1;
    private javax.swing.JButton txtBuyNum4_2;
    private javax.swing.JButton txtBuyNum4_3;
    private javax.swing.JButton txtBuyNum4_4;
    private javax.swing.JButton txtBuyNum4_5;
    private javax.swing.JButton txtBuyNum4_6;
    private javax.swing.JButton txtBuyNum5_1;
    private javax.swing.JButton txtBuyNum5_2;
    private javax.swing.JButton txtBuyNum5_3;
    private javax.swing.JButton txtBuyNum5_4;
    private javax.swing.JButton txtBuyNum5_5;
    private javax.swing.JButton txtBuyNum5_6;
    private javax.swing.JTextField txtID;
    private javax.swing.JButton txtSelNum1_1;
    private javax.swing.JButton txtSelNum1_2;
    private javax.swing.JButton txtSelNum1_3;
    private javax.swing.JButton txtSelNum1_4;
    private javax.swing.JButton txtSelNum1_5;
    private javax.swing.JButton txtSelNum1_6;
    private javax.swing.JButton txtSelNum2_1;
    private javax.swing.JButton txtSelNum2_2;
    private javax.swing.JButton txtSelNum2_3;
    private javax.swing.JButton txtSelNum2_4;
    private javax.swing.JButton txtSelNum2_5;
    private javax.swing.JButton txtSelNum2_6;
    private javax.swing.JButton txtSelNum3_1;
    private javax.swing.JButton txtSelNum3_2;
    private javax.swing.JButton txtSelNum3_3;
    private javax.swing.JButton txtSelNum3_4;
    private javax.swing.JButton txtSelNum3_5;
    private javax.swing.JButton txtSelNum3_6;
    private javax.swing.JButton txtSelNum4_1;
    private javax.swing.JButton txtSelNum4_2;
    private javax.swing.JButton txtSelNum4_3;
    private javax.swing.JButton txtSelNum4_4;
    private javax.swing.JButton txtSelNum4_5;
    private javax.swing.JButton txtSelNum4_6;
    private javax.swing.JButton txtSelNum5_1;
    private javax.swing.JButton txtSelNum5_2;
    private javax.swing.JButton txtSelNum5_3;
    private javax.swing.JButton txtSelNum5_4;
    private javax.swing.JButton txtSelNum5_5;
    private javax.swing.JButton txtSelNum5_6;
    // End of variables declaration//GEN-END:variables
}
