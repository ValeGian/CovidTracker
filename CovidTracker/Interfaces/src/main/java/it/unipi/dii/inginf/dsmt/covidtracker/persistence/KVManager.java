package it.unipi.dii.inginf.dsmt.covidtracker.persistence;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
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
import static org.iq80.leveldb.impl.Iq80DBFactory.*;


public class KVManager {
    private static final long startingPoint = (long) 1000 * 60 * 60 * 24 * 365 * 50;

public static void main(String[] args){
    KVManager kv = new KVManager();
    AggregationRequest a = new AggregationRequest();
    a.setStartDay("28/12/2020");
    a.setLastDay("31/12/2020");
    kv.saveAggregation(a, 4);
    Double d = kv.getAggregation(a);
    System.out.println(kv.getAggregation(a));

}

    private static DB openDB() {
        DB db = null;
        Options options = new Options();
        options.createIfMissing(true);
        try {
            db = factory.open(new File("myKV_DB"), options);
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
                for (iterator.seek(bytes(startId)); iterator.hasNext(); iterator.next()) {
                    String key = asString(iterator.peekNext().getKey());
                    String[] keySplit = key.split(":");

                    if (!keySplit[1].equals(type))
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
    public double getAggregation(AggregationRequest aggregation){
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
}
