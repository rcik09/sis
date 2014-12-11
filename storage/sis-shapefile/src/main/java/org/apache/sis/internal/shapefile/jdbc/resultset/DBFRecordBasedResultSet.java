package org.apache.sis.internal.shapefile.jdbc.resultset;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.sql.Date;
import java.sql.ResultSetMetaData;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;

import org.apache.sis.internal.shapefile.jdbc.SQLConnectionClosedException;
import org.apache.sis.internal.shapefile.jdbc.metadata.DBFResultSetMataData;
import org.apache.sis.internal.shapefile.jdbc.sql.*;
import org.apache.sis.internal.shapefile.jdbc.statement.DBFStatement;
import org.apache.sis.storage.shapefile.FieldDescriptor;

/**
 * A ResultSet based on a record.
 * @author Marc LE BIHAN
 */
public class DBFRecordBasedResultSet extends AbstractResultSet {
    /** The current record. */
    private Map<String, Object> m_record;
    
    /** Condition of where clause (currently, only one is handled). */
    private ConditionalClauseResolver m_singleConditionOfWhereClause;

    /** UTF-8 charset. */
    private static Charset UTF8 = Charset.forName("UTF-8");
    
    /**
     * Constructs a result set.
     * @param stmt Parent statement.
     * @param sqlQuery SQL Statment that produced this ResultSet.
     * @throws SQLInvalidStatementException if the SQL Statement is invalid.
     */
    public DBFRecordBasedResultSet(final DBFStatement stmt, String sqlQuery) throws SQLInvalidStatementException {
        super(stmt, sqlQuery);
        m_singleConditionOfWhereClause = new CrudeSQLParser(this).parse();
    }
    
    /**
     * @see org.apache.sis.internal.shapefile.jdbc.resultset.AbstractUnimplementedFeaturesOfResultSet#getBigDecimal(java.lang.String)
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field looked for doesn't exist.
     * @throws SQLNotNumericException if the field value is not numeric.
     */
    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLConnectionClosedException, SQLNoSuchFieldException, SQLNotNumericException {
        logStep("getBigDecimal", columnLabel);
        
        assertNotClosed();
        
        // Act as if we were a double, but store the result in a pre-created BigDecimal at the end.
        FieldDescriptor field = getField(columnLabel); // Ensure that the field queried exists.
        
        MathContext mc = new MathContext(field.getDecimalCount(), RoundingMode.HALF_EVEN);
        Double doubleValue = getDouble(columnLabel);
        
        if (doubleValue != null) {
            BigDecimal number = new BigDecimal(doubleValue, mc);
            m_wasNull = false;
            return number;
        }
        else {
            m_wasNull = true;
            return null;
        }
    }

    /**
     * @see java.sql.ResultSet#getBigDecimal(int)
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field looked for doesn't exist.
     * @throws SQLNotNumericException if the field value is not numeric.
     * @throws SQLIllegalColumnIndexException if the column index has an illegal value.
     */
    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLConnectionClosedException, SQLNoSuchFieldException, SQLNotNumericException, SQLIllegalColumnIndexException {
        logStep("getBigDecimal", columnIndex);
        return getBigDecimal(getField(columnIndex).getName());
    }

    /**
     * @see java.sql.ResultSet#getBigDecimal(java.lang.String, int)
     * @deprecated Deprecated API (from ResultSet Interface)
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field looked for doesn't exist.
     * @throws SQLNotNumericException if the field value is not numeric.
     */
    @Deprecated @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLConnectionClosedException, SQLNoSuchFieldException, SQLNotNumericException {
        logStep("getBigDecimal", columnLabel, scale);
        assertNotClosed();
        
        // Act as if we were a double, but store the result in a pre-created BigDecimal at the end.
        MathContext mc = new MathContext(scale, RoundingMode.HALF_EVEN);
        Double doubleValue = getDouble(columnLabel);
        
        if (doubleValue != null) {
            BigDecimal number = new BigDecimal(getDouble(columnLabel), mc);
            m_wasNull = false;
            return number;
        }
        else {
            m_wasNull = true;
            return null;
        }
    }

    /**
     * @see java.sql.ResultSet#getDate(java.lang.String)
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field looked for doesn't exist.
     * @throws SQLNotDateException if the field is not a date. 
     */
    @Override
    public Date getDate(String columnLabel) throws SQLConnectionClosedException, SQLNoSuchFieldException, SQLNotDateException {
        logStep("getDate", columnLabel);
        assertNotClosed();
        
        String value = getString(columnLabel);
        
        if (value == null || value.equals("00000000")) { // "00000000" is stored in Database to represent a null value too.
            m_wasNull = true;
            return null; // The ResultSet:getDate() contract is to return null when a null date is encountered.
        }
        else {
            m_wasNull = false;
        }
        
        // The DBase 3 date format is "YYYYMMDD".
        // if the length of the string isn't eight characters, the field format is incorrect.
        if (value.length() != 8) {
            String message = format(Level.SEVERE, "excp.field_is_not_a_date", columnLabel, m_sql, value);
            throw new SQLNotDateException(message, m_sql, getDatabase().getFile(), columnLabel, value);
        }

        // Extract the date parts.
        int year, month, dayOfMonth;
        
        try {
            year = Integer.parseInt(value.substring(0, 4));
            month = Integer.parseInt(value.substring(5, 7));
            dayOfMonth = Integer.parseInt(value.substring(7));
        }
        catch(NumberFormatException e) {
            String message = format(Level.SEVERE, "excp.field_is_not_a_date", columnLabel, m_sql, value);
            throw new SQLNotDateException(message, m_sql, getDatabase().getFile(), columnLabel, value);
        }
        
        // Create a date.
        Calendar calendar = new GregorianCalendar(year, month-1, dayOfMonth, 0, 0, 0);
        Date sqlDate = new Date(calendar.getTimeInMillis());
        return sqlDate;
    }

    /**
     * @see java.sql.ResultSet#getDate(int)
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field looked for doesn't exist.
     * @throws SQLNotDateException if the field is not a date. 
     * @throws SQLIllegalColumnIndexException if the column index has an illegal value.
     */
    @Override
    public Date getDate(int columnIndex) throws SQLConnectionClosedException, SQLNoSuchFieldException, SQLNotDateException, SQLIllegalColumnIndexException {
        logStep("getDate", columnIndex);
        return getDate(getField(columnIndex).getName());
    }

    /**
     * @see java.sql.ResultSet#getDouble(java.lang.String)
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field looked for doesn't exist.
     * @throws SQLNotNumericException if the field value is not numeric.
     */
    @Override
    public double getDouble(String columnLabel) throws SQLConnectionClosedException, SQLNoSuchFieldException, SQLNotNumericException {
        logStep("getDouble", columnLabel);
        
        Double value = getNumeric(columnLabel, Double::parseDouble);
        m_wasNull = (value == null);
        return value != null ? value : 0.0; // The ResultSet contract for numbers is to return 0 when a null value is encountered.
    }

    /**
     * @see java.sql.ResultSet#getDouble(int)
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field looked for doesn't exist.
     * @throws SQLNotNumericException if the field value is not numeric.
     * @throws SQLIllegalColumnIndexException if the column index has an illegal value.
     */
    @Override
    public double getDouble(int columnIndex) throws SQLConnectionClosedException, SQLNoSuchFieldException, SQLNotNumericException, SQLIllegalColumnIndexException {
        logStep("getDouble", columnIndex);
        return getDouble(getField(columnIndex).getName());
    }

    /**
     * @see java.sql.ResultSet#getFloat(java.lang.String)
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field looked for doesn't exist.
     * @throws SQLNotNumericException if the field value is not numeric.
     */
    @Override
    public float getFloat(String columnLabel) throws SQLConnectionClosedException, SQLNoSuchFieldException, SQLNotNumericException {
        logStep("getFloat", columnLabel);
        
        Float value = getNumeric(columnLabel, Float::parseFloat);
        m_wasNull = (value == null);
        return value != null ? value : 0; // The ResultSet contract for numbers is to return 0 when a null value is encountered.
    }

    /**
     * @see java.sql.ResultSet#getFloat(int)
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field looked for doesn't exist.
     * @throws SQLNotNumericException if the field value is not numeric.
     * @throws SQLIllegalColumnIndexException if the column index has an illegal value.
     */
    @Override
    public float getFloat(int columnIndex) throws SQLConnectionClosedException, SQLNoSuchFieldException, SQLNotNumericException, SQLIllegalColumnIndexException {
        logStep("getFloat", columnIndex);
        return getFloat(getField(columnIndex).getName());
    }

    /**
     * @see org.apache.sis.internal.shapefile.jdbc.resultset.AbstractUnimplementedFeaturesOfResultSet#getInt(java.lang.String)
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field looked for doesn't exist.
     * @throws SQLNotNumericException if the field value is not numeric.
     */
    @Override
    public int getInt(String columnLabel) throws SQLConnectionClosedException, SQLNoSuchFieldException, SQLNotNumericException {
        logStep("getInt", columnLabel);
        
        Integer value = getNumeric(columnLabel, Integer::parseInt);
        m_wasNull = (value == null);
        return value != null ? value : 0; // The ResultSet contract for numbers is to return 0 when a null value is encountered.
    }

    /**
     * @see java.sql.ResultSet#getInt(int)
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field looked for doesn't exist.
     * @throws SQLNotNumericException if the field value is not numeric.
     * @throws SQLIllegalColumnIndexException if the column index has an illegal value.
     */
    @Override
    public int getInt(int columnIndex) throws SQLConnectionClosedException, SQLNoSuchFieldException, SQLNotNumericException, SQLIllegalColumnIndexException {
        logStep("getInt", columnIndex);
        return getInt(getField(columnIndex).getName());
    }

    /**
     * @see java.sql.ResultSet#getLong(java.lang.String)
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field looked for doesn't exist.
     * @throws SQLNotNumericException if the field value is not numeric.
     */
    @Override
    public long getLong(String columnLabel) throws SQLConnectionClosedException, SQLNoSuchFieldException, SQLNotNumericException {
        logStep("getLong", columnLabel);
        
        Long value = getNumeric(columnLabel, Long::parseLong);
        m_wasNull = (value == null);
        return value != null ? value : 0; // The ResultSet contract for numbers is to return 0 when a null value is encountered.
    }

    /**
     * @see java.sql.ResultSet#getLong(int)
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field looked for doesn't exist.
     * @throws SQLNotNumericException if the field value is not numeric.
     * @throws SQLIllegalColumnIndexException if the column index has an illegal value.
     */
    @Override public long getLong(int columnIndex) throws SQLConnectionClosedException, SQLNoSuchFieldException, SQLNotNumericException, SQLIllegalColumnIndexException {
        logStep("getLong", columnIndex);
        return getLong(getField(columnIndex).getName());
    }
    
    /**
     * @see java.sql.ResultSet#getMetaData()
     */
    @Override
    public ResultSetMetaData getMetaData() {
        logStep("getMetaData");
        
        DBFResultSetMataData meta = new DBFResultSetMataData(this); 
        return meta;
    }

    /**
     * @see java.sql.ResultSet#getShort(java.lang.String)
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field looked for doesn't exist.
     * @throws SQLNotNumericException if the field value is not numeric or has a NULL value.
     */
    @Override
    public short getShort(String columnLabel) throws SQLConnectionClosedException, SQLNoSuchFieldException, SQLNotNumericException {
        logStep("getShort", columnLabel);
        
        Short value = getNumeric(columnLabel, Short::parseShort);
        m_wasNull = (value == null);
        return value != null ? value : 0; // The ResultSet contract for numbers is to return 0 when a null value is encountered.
    }

    /**
     * @see java.sql.ResultSet#getShort(int)
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field looked for doesn't exist.
     * @throws SQLNotNumericException if the field value is not numeric or has a NULL value.
     * @throws SQLIllegalColumnIndexException if the column index has an illegal value.
     */
    @Override
    public short getShort(int columnIndex) throws SQLConnectionClosedException, SQLNoSuchFieldException, SQLNotNumericException, SQLIllegalColumnIndexException {
        logStep("getShort", columnIndex);
        return getShort(getField(columnIndex).getName());
    }

    /**
     * Returns the value in the current row for the given column.
     * @param columnLabel Column name.
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field does not exist.
     */
    @Override
    public String getString(String columnLabel) throws SQLConnectionClosedException, SQLNoSuchFieldException {
        logStep("getString", columnLabel);
        assertNotClosed();
        
        getField(columnLabel); // Ensure that the field queried exists, else a null value here can be interpreted as "not existing" or "has a null value".
        String withoutCharset = (String)m_record.get(columnLabel);
        
        if (withoutCharset == null) {
            m_wasNull = true;
            return withoutCharset;
        } 
        else {
            m_wasNull = false;
        }
        
        // If a non null value has been readed, convert it to the wished Charset.
        String withDatabaseCharset = new String(withoutCharset.getBytes(), getDatabase().getCharset()); 
        log(Level.FINER, "log.string_field_charset", columnLabel, withoutCharset, withDatabaseCharset, getDatabase().getCharset());
        
        // Because the Database is old (end of 1980's), it has not been made to support UTF-8 encoding.
        // But must users of DBase 3 don't know this, and sometimes a String field may carry such characters.
        // Attempt to determine if the string could be an UTF-8 String instead.
        String withUtf8Encoding = new String(withoutCharset.getBytes(), UTF8);

        // If conversion contains a not convertible character, it's not an UTF-8 string.
        // If the UTF-8 string is shorter than the one that would have given the database charset, it's a good sign : it has chances to be better.
        boolean unsureResult = withUtf8Encoding.indexOf('\ufffd') != -1 || withUtf8Encoding.length() >= withDatabaseCharset.length();
        
        if (unsureResult)
            return withDatabaseCharset;
        else {
            log(Level.FINER, "log.string_field_charset", columnLabel, withoutCharset, withUtf8Encoding, UTF8);
            return withUtf8Encoding;
        }
    }
    
    /**
     * @see java.sql.ResultSet#getString(int)
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field looked for doesn't exist.
     * @throws SQLIllegalColumnIndexException if the column index has an illegal value.
     */
    @Override
    public String getString(int columnIndex) throws SQLConnectionClosedException, SQLNoSuchFieldException, SQLIllegalColumnIndexException {
        logStep("getString", columnIndex);
        return(getString(getField(columnIndex).getName()));
    }

    /**
     * Moves the cursor forward one row from its current position.
     * @throws SQLInvalidStatementException if the SQL statement is invalid.
     * @throws SQLIllegalParameterException if the value of one parameter of a condition is invalid.
     * @throws SQLNoSuchFieldException if a field mentionned in the condition doesn't exist.
     * @throws SQLUnsupportedParsingFeatureException if the caller asked for a not yet supported feature of the driver.
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNotNumericException if a value or data expected to be numeric isn't.
     * @throws SQLNotDateException if a value or data expected to be a date isn't.
     */
    @Override
    public boolean next() throws SQLNoResultException, SQLConnectionClosedException, SQLInvalidStatementException, SQLIllegalParameterException, SQLNoSuchFieldException, SQLUnsupportedParsingFeatureException, SQLNotNumericException, SQLNotDateException {
        logStep("next");
        assertNotClosed();

        // Check that we aren't at the end of the Database file.
        final int remaining = getDatabase().getRecordCount() - getDatabase().getRowNum();
        
        if (remaining <= 0) {
            throw new SQLNoResultException(format(Level.SEVERE, "excp.no_more_results", m_sql, getDatabase().getFile().getName()), m_sql, getDatabase().getFile());
        }

        //m_record = getDatabase().readNextRowAsObjects();
        return nextRecordMatchingConditions();
    }
    
    /**
     * Find the next record that match the where condition.
     * @return true if a record has been found.
     * @throws SQLInvalidStatementException if the SQL statement is invalid.
     * @throws SQLIllegalParameterException if the value of one parameter of a condition is invalid.
     * @throws SQLNoSuchFieldException if a field mentionned in the condition doesn't exist.
     * @throws SQLUnsupportedParsingFeatureException if the caller asked for a not yet supported feature of the driver.
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNotNumericException if a value or data expected to be numeric isn't.
     * @throws SQLNotDateException if a value or data expected to be a date isn't.
     */
    private boolean nextRecordMatchingConditions() throws SQLInvalidStatementException, SQLIllegalParameterException, SQLNoSuchFieldException, SQLUnsupportedParsingFeatureException, SQLConnectionClosedException, SQLNotNumericException, SQLNotDateException {
        boolean recordMatchesConditions = false;
        
        while(getDatabase().getRecordCount() - getDatabase().getRowNum() > 0 && recordMatchesConditions == false) {
            m_record = getDatabase().readNextRowAsObjects();
            recordMatchesConditions = m_singleConditionOfWhereClause == null || m_singleConditionOfWhereClause.isVerified(this);
        }
        
        return recordMatchesConditions && 
               (getDatabase().getRecordCount() - getDatabase().getRowNum() > 0); // Beware of the end of database !
    }
    
    /**
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) {
        logStep("isWrapperFor", iface);
        return iface.isAssignableFrom(getInterface());
    }

    /**
     * @see java.sql.ResultSet#wasNull()
     */
    @Override
    public boolean wasNull() {
        logStep("wasNull");
        return m_wasNull;
    }

    /**
     * Get a numeric value.
     * @param <T> Type of the number.
     * @param columnLabel Column Label.
     * @param parse Parsing function : Integer.parseInt, Float.parseFloat, Long.parseLong, ...
     * @return The expected value or null if null was encountered.
     * @throws SQLConnectionClosedException if the connection is closed.
     * @throws SQLNoSuchFieldException if the field looked for doesn't exist.
     * @throws SQLNotNumericException if the field value is not numeric or has a NULL value.
     */
    private <T extends Number> T getNumeric(String columnLabel, Function<String, T> parse) throws SQLConnectionClosedException, SQLNoSuchFieldException, SQLNotNumericException {
        assertNotClosed();
        
        FieldDescriptor field = getField(columnLabel); // Ensure that the field queried exists, else, a null value here can be interpreted as "not existing" or "has a null value".
        String textValue = (String)m_record.get(columnLabel); 
        
        if (textValue == null) {
            return null;
        }
        
        try {
            textValue = textValue.trim(); // Field must be trimed before being converted.
            T value = parse.apply(textValue);
            return(value);
        } 
        catch(NumberFormatException e) {
            String message = format(Level.SEVERE, "excp.field_is_not_numeric", columnLabel, field.getType(), m_sql, textValue);
            throw new SQLNotNumericException(message, m_sql, getDatabase().getFile(), columnLabel, textValue);
        }
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return format("toString", m_statement != null ? m_statement.toString() : null, m_sql, isClosed() == false);
    }
}
