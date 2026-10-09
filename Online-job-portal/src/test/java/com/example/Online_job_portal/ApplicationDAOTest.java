package com.example.Online_job_portal;

import com.example.Online_job_portal.dao.ApplicationDAO;
import com.example.Online_job_portal.model.Application;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("ApplicationDAO & ACID Transaction Tests")
class ApplicationDAOTest {

    @Test
    @DisplayName("Saves application to in-memory store")
    void savesApplication() {
        ApplicationDAO applicationDAO = new ApplicationDAO();
        applicationDAO.save(new Application(1, 1, 3, "resume.pdf", "SUBMITTED", LocalDate.now()));
        assertTrue(applicationDAO.findById(1).isPresent());
    }

    @Test
    @DisplayName("JDBC ACID: Transaction rolls back on database failure and leaves state clean")
    void testTransactionRollbackOnDatabaseFailure() throws SQLException {
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);

        // Simulate failure on executeUpdate (e.g. disk failure, foreign key violation)
        when(mockConnection.prepareStatement(anyString(), anyInt())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenThrow(new SQLException("Simulated DB integrity constraint violation"));

        Application app = new Application(0, 10, 5, "candidate_resume.pdf", "SUBMITTED", LocalDate.now());
        boolean success = applicationDAO.executeTransaction(mockConnection, app, "Unit Test", 2);

        // Assert transaction failure
        assertFalse(success, "Transaction must return false on SQLException");

        // Verify setAutoCommit(false) was invoked
        verify(mockConnection).setAutoCommit(false);

        // Verify rollback was triggered
        verify(mockConnection).rollback();

        // Verify commit was NEVER called
        verify(mockConnection, never()).commit();
    }

    @Test
    @DisplayName("JDBC ACID: Transaction atomically commits when all 3 steps succeed")
    void testTransactionCommitOnSuccess() throws SQLException {
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);

        when(mockConnection.prepareStatement(anyString(), anyInt())).thenReturn(mockStatement);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.getGeneratedKeys()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true);
        when(mockRs.getInt(1)).thenReturn(777);

        Application app = new Application(0, 10, 5, "candidate_resume.pdf", "SUBMITTED", LocalDate.now());
        boolean success = applicationDAO.executeTransaction(mockConnection, app, "Unit Test Commit", 2);

        assertTrue(success, "Transaction must return true when all statements succeed");
        verify(mockConnection).setAutoCommit(false);
        verify(mockConnection).commit();
        verify(mockConnection, never()).rollback();
    }
}
