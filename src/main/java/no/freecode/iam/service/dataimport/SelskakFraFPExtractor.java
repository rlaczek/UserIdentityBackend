package no.obos.iam.service.dataimport;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Henter selskaper fra forvaltningsportalen og skriver til selskaper.csv.
 */
public class SelskakFraFPExtractor {
    private static final String SQL = "select selsnr,navn from FP_SELS";

    public static void main(String[] args) throws SQLException, IOException {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("com.ibm.as400.access.AS400JDBCDriver");
        dataSource.setUsername("fp000us");
        dataSource.setPassword("in98a62");
        dataSource.setUrl("jdbc:as400://obos.obos.no;naming=system");
        QueryRunner queryRunner = new QueryRunner(dataSource);
        String result = queryRunner.query(SQL, new ResultSetHandler<String>() {
            @Override
            public String handle(ResultSet rs) throws SQLException {
                StringBuilder export = new StringBuilder();
                while(rs.next()) {
                    String selsnr = rs.getString(1);
                    String navn = rs.getString(2);
                    if(navn != null && navn.trim().length() > 0 && !navn.contains("Utgått")) {
                        export.append(selsnr).append(",").append(navn).append("\n");
                    }
                }
                return export.toString();
            }
        });
        FileWriter fw = null;
        try {
            fw = new FileWriter("selskaper.csv");
            fw.write(result);

        } finally {
            if(fw != null) {
                fw.close();
            }
        }
    }
}
