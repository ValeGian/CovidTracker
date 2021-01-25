package it.unipi.dii.inginf.dsmt.covidtracker.persistence;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.KVManager;
import org.iq80.leveldb.*;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.iq80.leveldb.impl.Iq80DBFactory.*;


public class KVManagerImpl implements KVManager {
    private static final long startingPoint = (long) 1000 * 60 * 60 * 24 * 365 * 50;
    private String fileName;

    public KVManagerImpl(String myName) {
        fileName = myName + "KVDb";
    }

    public static void main(String[] args){
        KVManagerImpl kv = new KVManagerImpl("prova");
        kv.deleteAllInfo();
        kv.populateDb(100);
        System.out.println("STAMPO TUTTO:\n" + kv.getAllInfo());

    }

    public void populateDb(int bound){

        Random myRandom = new Random();
        for(int i = 1; i <= 31; i++) {
            DailyReport newDaily = new DailyReport();
            newDaily.addTotalSwab(myRandom.nextInt(bound) + 50);
            newDaily.addTotalNegative(myRandom.nextInt(bound) + 50);
            newDaily.addTotalPositive(myRandom.nextInt(bound) + 50);
            newDaily.addTotalDead(myRandom.nextInt(bound) + 50);

            try {
                Date dateToInsert;
                if(i < 10)
                    dateToInsert = new SimpleDateFormat("dd/MM/yyyy").parse("0" + i + "/01/2021");
                else
                    dateToInsert = new SimpleDateFormat("dd/MM/yyyy").parse(i + "/01/2021");
                addFake(newDaily, dateToInsert);
            } catch (ParseException parseException) {
                parseException.printStackTrace();
            }

        }
    }


    public boolean addFake(DailyReport dailyReport, Date initialDate){
        try (DB db = openDB();
             WriteBatch batch = db.createWriteBatch()) {


            String startId = String.valueOf(initialDate.getTime() - startingPoint);

            batch.put(bytes(startId + ":" + "swab"), bytes(String.valueOf(dailyReport.getTotalSwab())));
            batch.put(bytes(startId + ":" + "positive"), bytes(String.valueOf(dailyReport.getTotalPositive())));
            batch.put(bytes(startId + ":" + "negative"), bytes(String.valueOf(dailyReport.getTotalNegative())));
            batch.put(bytes(startId + ":" + "dead"), bytes(String.valueOf(dailyReport.getTotalDead())));
            db.write(batch);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    private DB openDB() {
        DB db = null;
        Options options = new Options();
        options.createIfMissing(true);
        try {
            db = factory.open(new File(fileName), options);
        } catch (IOException ioe) {
            System.err.println("Connection Failed!\n");
        } finally {
            return db;
        }
    }

    private DB openClientRequestDB() {
        DB db = null;
        Options options = new Options();
        options.createIfMissing(true);
        try {
            db = factory.open(new File(fileName + "ClientRequest"), options);
        } catch (IOException ioe) {
            System.err.println("Connection Failed!\n");
        } finally {
            return db;
        }
    }

    public boolean addDailyReport(DailyReport dailyReport) {
        try (DB db = openDB();
             WriteBatch batch = db.createWriteBatch()) {
            ZonedDateTime startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault());
            long todayMillis = startOfToday.toEpochSecond() * 1000;

            long millisecond = todayMillis - startingPoint;

            batch.put(bytes(millisecond + ":" + "swab"), bytes(String.valueOf(dailyReport.getTotalSwab())));
            batch.put(bytes(millisecond + ":" + "positive"), bytes(String.valueOf(dailyReport.getTotalPositive())));
            batch.put(bytes(millisecond + ":" + "negative"), bytes(String.valueOf(dailyReport.getTotalNegative())));
            batch.put(bytes(millisecond + ":" + "dead"), bytes(String.valueOf(dailyReport.getTotalDead())));
            db.write(batch);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public List<Integer> getDailyReportsInAPeriod(String initialDateS, String finalDateS, String type) {
        List<Integer> searchedReports = new ArrayList<>();

        try {
            Date initialDate = new SimpleDateFormat("dd/MM/yyyy").parse(initialDateS);
            Date finalDate = new SimpleDateFormat("dd/MM/yyyy").parse(finalDateS);

            String startId = String.valueOf(initialDate.getTime() - startingPoint);
            String lastId = String.valueOf(finalDate.getTime() - startingPoint);

            try (DB db = openDB(); DBIterator iterator = db.iterator()) {
                for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                    String key = asString(iterator.peekNext().getKey());
                    String[] keySplit = key.split(":");


                    if (Long.parseLong(keySplit[0]) < Long.parseLong(startId) || !keySplit[1].equals(type))
                        continue;


                    if (Long.parseLong(keySplit[0]) > Long.parseLong(lastId))
                        break;

                    String value = asString(iterator.peekNext().getValue());
                    searchedReports.add(Integer.valueOf(value));

                    if (keySplit[0].equals(lastId))
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
        return searchedReports;
    }

    /**
     * @param aggregation
     * @return -1 if not found or the aggregation's result
     */
    public double getAggregation(AggregationRequest aggregation) {
        String key = aggregation.toKey();
        double result = 0;

        try (DB db = openDB()) {
            result = Double.parseDouble(asString(db.get(bytes(key))));
        }catch (NullPointerException ex){
            return -1;
        } catch (DBException | IOException ex){
            ex.printStackTrace();
        }
        return result;
    }

    public void saveAggregation(AggregationRequest aggregation, double result) {
        String key = aggregation.toKey();

        try (DB db = openDB()){
            db.put(bytes(key), bytes(String.valueOf(result)));
        } catch (DBException | IOException ex) {
            ex.printStackTrace();
        }
    }

    public double getDailyReport(String day, String type) {
        List<Integer> result = getDailyReportsInAPeriod(day, day, type);
        if(!result.isEmpty())
            return result.get(0);

        return -1;
    }

    @Override
    public void deleteDailyReport(String day, String type) {

        try {
            Date initialDate = new SimpleDateFormat("dd/MM/yyyy").parse(day);
            String startId = String.valueOf(initialDate.getTime() - startingPoint);
            try(DB db = openDB()){
                db.delete(bytes(startId + ":" + type));
            }
        } catch (ParseException | IOException parseException) {
            parseException.printStackTrace();
        }

    }

    public void addClientRequest(String clientRequest){

        long millisecond = ZonedDateTime.now().toInstant().toEpochMilli() - startingPoint;

        try (DB db = openClientRequestDB()){
            db.put(bytes(String.valueOf(millisecond)), bytes(clientRequest));
        } catch (DBException | IOException ex) {
            ex.printStackTrace();
        }
    }

    public String getAllClientRequest() {

        List<String> clientRequest = new ArrayList<>();

        try (DB db = openClientRequestDB(); DBIterator iterator = db.iterator()) {
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                clientRequest.add(asString(iterator.peekNext().getValue()));
            }
        } catch(Exception e){
            e.printStackTrace();
            return "";
        }

        return listToString(clientRequest);
    }

    private String listToString(List<String> inputList) {
        String outMsg = "";
        for(String m : inputList) {
            outMsg += m+"\n";
        }
        return outMsg;
    }

    public void deleteAllClientRequest(){

        try (DB db = openClientRequestDB(); DBIterator iterator = db.iterator()) {
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                db.delete(iterator.peekNext().getKey());
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void deleteAllInfo(){

        try (DB db = openDB(); DBIterator iterator = db.iterator()) {
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                db.delete(iterator.peekNext().getKey());
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public String getAllInfo() {

        List<String> clientRequest = new ArrayList<>();

        try (DB db = openDB(); DBIterator iterator = db.iterator()) {
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                clientRequest.add("KEY: " + asString(iterator.peekNext().getKey()) + " VALUE: " + asString(iterator.peekNext().getValue()));
            }
        } catch(Exception e){
            e.printStackTrace();
            return "";
        }

        return listToString(clientRequest);
    }


}
