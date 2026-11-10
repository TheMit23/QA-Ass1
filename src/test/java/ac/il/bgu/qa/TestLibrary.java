package ac.il.bgu.qa;

import ac.il.bgu.qa.errors.*;
import ac.il.bgu.qa.services.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestLibrary {
    @Mock
    private DatabaseService mockDatabaseService;
    @Mock
    private ReviewService mockReviewService;
    @Mock
    private NotificationService mockNotificationService;

    private Book spyBook;
    private User spyUser;
    private Library library;

    // Valid inputs shared by all tests
    private static final String validInputIsbn = "978-0-306-40615-7";
    private static final String validInputTitle = "Clean Code";
    private static final String validInputAuthor = "Robert C. Martin";
    private static final String validInputUserName = "Israel Israeli";
    private static final String validInputUserId = "123456789012";

    @BeforeEach
    void setup(){
        MockitoAnnotations.openMocks(this);
        library = new Library(mockDatabaseService, mockReviewService);
        spyBook = spy(new Book(validInputIsbn, validInputTitle, validInputAuthor));
        spyUser = spy(new User(validInputUserName, validInputUserId, mockNotificationService));
    }

    // ===================== registerUser =====================

    @Test
    void GivenNullUser_WhenRegisterUser_ThenThrowIllegalArgumentException() {
        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(null));

        // Assert
        assertEquals("Invalid user.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "12345678901",      // 11 digits, one below the boundary
            "1234567890123",    // 13 digits, one above the boundary
            "12345678901a",     // 12 chars with a letter
            " 12345678901",     // 12 chars with a space
            "1234-5678901"      // 12 chars with a hyphen
    })
    void GivenInvalidUserId_WhenRegisterUser_ThenThrowIllegalArgumentException(String invalidUserId) {
        // Arrange
        doReturn(invalidUserId).when(spyUser).getId();

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(spyUser));

        // Assert
        assertEquals("Invalid user Id.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void GivenInvalidUserName_WhenRegisterUser_ThenThrowIllegalArgumentException(String invalidUserName) {
        // Arrange
        doReturn(invalidUserName).when(spyUser).getName();

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(spyUser));

        // Assert
        assertEquals("Invalid user name.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @Test
    void GivenNullNotificationService_WhenRegisterUser_ThenThrowIllegalArgumentException() {
        // Arrange
        doReturn(null).when(spyUser).getNotificationService();

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(spyUser));

        // Assert
        assertEquals("Invalid notification service.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @Test
    void GivenUserAlreadyExists_WhenRegisterUser_ThenThrowIllegalArgumentException() {
        // Arrange
        User existingUser = mock(User.class);
        when(mockDatabaseService.getUserById(validInputUserId)).thenReturn(existingUser);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(spyUser));

        // Assert
        assertEquals("User already exists.", exception.getMessage());
        verify(mockDatabaseService, never()).registerUser(anyString(), any(User.class));
    }

    @Test
    void GivenValidNewUser_WhenRegisterUser_ThenUserIsRegisteredInDatabase() {
        // Arrange - no user with this id exists yet
        when(mockDatabaseService.getUserById(validInputUserId)).thenReturn(null);

        // Act
        library.registerUser(spyUser);

        // Assert
        verify(mockDatabaseService, times(1)).getUserById(validInputUserId);
        verify(mockDatabaseService, times(1)).registerUser(validInputUserId, spyUser);
    }

    // ===================== addBook (also covers private isISBNValid, isAuthorValid) =====================

    @Test
    void GivenNullBook_WhenAddBook_ThenThrowIllegalArgumentException() {
        //
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(null));

        // Assert
        assertEquals("Invalid book.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "978030640615",       // 12 digits, one below the boundary
            "97803064061570",     // 14 digits, one above the boundary
            "978030640615X",      // 13 chars with a letter
            "978030640615 ",      // 13 chars with a space
            "9780306406158",      // 13 digits, wrong check digit
            "978-0-306-40615-8",  // hyphenated, wrong check digit
            "978-0-306-40615"     // hyphenated, only 12 digits after removing hyphens
    })
    void GivenInvalidIsbn_WhenAddBook_ThenThrowIllegalArgumentException(String invalidIsbn) {
        // Arrange
        doReturn(invalidIsbn).when(spyBook).getISBN();

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(spyBook));

        // Assert
        assertEquals("Invalid ISBN.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "9780306406157",      // plain 13 digits
            "9783161484100"       // calculated check digit is 10, which must wrap to 0
    })
    void GivenValidIsbn_WhenAddBook_ThenBookIsAddedToDatabase(String validIsbn) {
        // Arrange
        doReturn(validIsbn).when(spyBook).getISBN();

        // Act
        library.addBook(spyBook);

        // Assert
        verify(mockDatabaseService, times(1)).addBook(validIsbn, spyBook);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void GivenInvalidTitle_WhenAddBook_ThenThrowIllegalArgumentException(String invalidTitle) {
        // Arrange
        doReturn(invalidTitle).when(spyBook).getTitle();

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(spyBook));

        // Assert
        assertEquals("Invalid title.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "-John", " John", ".John", "'John", "1John",  // does not start with a letter
            "John-", "John ", "John.", "John'", "John1",  // does not end with a letter
            "Jo#hn", "J0hn", "Jo_hn",                     // contains a forbidden character
            "Jean--Paul",                                 // consecutive hyphens
            "O''Brien",                                   // consecutive apostrophes
            "John  Smith",                                // consecutive spaces
            "J..R"                                        // consecutive dots
    })
    void GivenInvalidAuthor_WhenAddBook_ThenThrowIllegalArgumentException(String invalidAuthor) {
        // Arrange
        doReturn(invalidAuthor).when(spyBook).getAuthor();

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(spyBook));

        // Assert
        assertEquals("Invalid author.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "J",                  // single letter, the character loop does not run
            "Jean-Paul",          // hyphen is allowed
            "O'Brien",            // apostrophe is allowed
            "John Smith",         // space is allowed
            "J.R",                // dot is allowed
            "J. R",               // two different special characters in a row are allowed
            "Jos\u00E9"           // "Jose" with an accent: non-ASCII letters are letters (escaped to keep the file ASCII-only)
    })
    void GivenValidAuthor_WhenAddBook_ThenBookIsAddedToDatabase(String validAuthor) {
        // Arrange
        doReturn(validAuthor).when(spyBook).getAuthor();

        // Act
        library.addBook(spyBook);

        // Assert
        verify(mockDatabaseService, times(1)).addBook(validInputIsbn, spyBook);
    }

    @Test
    void GivenBorrowedBook_WhenAddBook_ThenThrowIllegalArgumentException() {
        // Arrange
        doReturn(true).when(spyBook).isBorrowed();

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(spyBook));

        // Assert
        assertEquals("Book with invalid borrowed state.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @Test
    void GivenBookAlreadyExists_WhenAddBook_ThenThrowIllegalArgumentException() {
        // Arrange - the database already holds a book with the same ISBN
        Book existingBook = mock(Book.class);
        when(mockDatabaseService.getBookByISBN(validInputIsbn)).thenReturn(existingBook);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(spyBook));

        // Assert
        assertEquals("Book already exists.", exception.getMessage());
        verify(mockDatabaseService, never()).addBook(anyString(), any(Book.class));
    }

    @Test
    void GivenValidNewBook_WhenAddBook_ThenBookIsAddedToDatabase() {
        // Arrange - no book with this ISBN exists yet
        when(mockDatabaseService.getBookByISBN(validInputIsbn)).thenReturn(null);

        // Act
        library.addBook(spyBook);

        // Assert
        verify(mockDatabaseService, times(1)).getBookByISBN(validInputIsbn);
        verify(mockDatabaseService, times(1)).addBook(validInputIsbn, spyBook);
    }

    // ===================== notifyUserWithBookReviews =====================

    // Reviews returned by the review service, and the message Library is expected to build from them.
    private static final List<String> validReviews = Arrays.asList("Great book", "Must read");
    private static final String expectedNotificationMessage =
            "Reviews for '" + validInputTitle + "':\nGreat book\nMust read";

    /**
     * Stubs the database and review service so that every check in
     * notifyUserWithBookReviews passes. Tests override single stubs to break one step.
     */
    private void arrangeValidBookUserAndReviews() {
        when(mockDatabaseService.getBookByISBN(validInputIsbn)).thenReturn(spyBook);
        when(mockDatabaseService.getUserById(validInputUserId)).thenReturn(spyUser);
        when(mockReviewService.getReviewsForBook(validInputIsbn)).thenReturn(validReviews);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "978030640615",       // 12 digits
            "9780306406158"       // wrong check digit
    })
    void GivenInvalidIsbn_WhenNotifyUserWithBookReviews_ThenThrowIllegalArgumentException(String invalidIsbn) {
        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.notifyUserWithBookReviews(invalidIsbn, validInputUserId));

        // Assert
        assertEquals("Invalid ISBN.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService, mockReviewService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "12345678901",        // 11 digits, one below the boundary
            "1234567890123",      // 13 digits, one above the boundary
            "12345678901a"        // 12 chars with a letter
    })
    void GivenInvalidUserId_WhenNotifyUserWithBookReviews_ThenThrowIllegalArgumentException(String invalidUserId) {
        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.notifyUserWithBookReviews(validInputIsbn, invalidUserId));

        // Assert
        assertEquals("Invalid user Id.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService, mockReviewService);
    }

    @Test
    void GivenBookNotInDatabase_WhenNotifyUserWithBookReviews_ThenThrowBookNotFoundException() {
        // Arrange
        arrangeValidBookUserAndReviews();
        when(mockDatabaseService.getBookByISBN(validInputIsbn)).thenReturn(null);

        // Act
        BookNotFoundException exception = assertThrows(BookNotFoundException.class,
                () -> library.notifyUserWithBookReviews(validInputIsbn, validInputUserId));

        // Assert
        assertEquals("Book not found!", exception.getMessage());
        verify(mockDatabaseService, never()).getUserById(anyString());
        verifyNoInteractions(mockReviewService);
    }

    @Test
    void GivenUserNotInDatabase_WhenNotifyUserWithBookReviews_ThenThrowUserNotRegisteredException() {
        // Arrange
        arrangeValidBookUserAndReviews();
        when(mockDatabaseService.getUserById(validInputUserId)).thenReturn(null);

        // Act
        UserNotRegisteredException exception = assertThrows(UserNotRegisteredException.class,
                () -> library.notifyUserWithBookReviews(validInputIsbn, validInputUserId));

        // Assert
        assertEquals("User not found!", exception.getMessage());
        verifyNoInteractions(mockReviewService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void GivenNoReviews_WhenNotifyUserWithBookReviews_ThenThrowNoReviewsFoundException(List<String> noReviews) {
        // Arrange
        arrangeValidBookUserAndReviews();
        when(mockReviewService.getReviewsForBook(validInputIsbn)).thenReturn(noReviews);

        // Act
        NoReviewsFoundException exception = assertThrows(NoReviewsFoundException.class,
                () -> library.notifyUserWithBookReviews(validInputIsbn, validInputUserId));

        // Assert - the connection is still closed, and the user is not notified
        assertEquals("No reviews found!", exception.getMessage());
        verify(mockReviewService, times(1)).close();
        verify(spyUser, never()).sendNotification(anyString());
    }

    @Test
    void GivenReviewServiceFails_WhenNotifyUserWithBookReviews_ThenThrowReviewServiceUnavailableException() {
        // Arrange
        arrangeValidBookUserAndReviews();
        when(mockReviewService.getReviewsForBook(validInputIsbn))
                .thenThrow(new ReviewException("Connection lost"));

        // Act
        ReviewServiceUnavailableException exception = assertThrows(ReviewServiceUnavailableException.class,
                () -> library.notifyUserWithBookReviews(validInputIsbn, validInputUserId));

        // Assert - the connection is still closed, and the user is not notified
        assertEquals("Review service unavailable!", exception.getMessage());
        verify(mockReviewService, times(1)).close();
        verify(spyUser, never()).sendNotification(anyString());
    }

    @Test
    void GivenValidBookUserAndReviews_WhenNotifyUserWithBookReviews_ThenUserIsNotifiedWithReviews() {
        // Arrange
        arrangeValidBookUserAndReviews();

        // Act
        library.notifyUserWithBookReviews(validInputIsbn, validInputUserId);

        // Assert - reviews are fetched, the connection is closed, then the user gets the exact message once
        InOrder inOrder = inOrder(mockReviewService, spyUser);
        inOrder.verify(mockReviewService).getReviewsForBook(validInputIsbn);
        inOrder.verify(mockReviewService).close();
        inOrder.verify(spyUser).sendNotification(expectedNotificationMessage);
        verify(mockReviewService, times(1)).close();
        verify(spyUser, times(1)).sendNotification(anyString());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void GivenNotificationFailsFewerThanFiveTimes_WhenNotifyUserWithBookReviews_ThenRetryUntilSuccess(int failures) {
        // Arrange - sending fails `failures` times in a row, then succeeds
        arrangeValidBookUserAndReviews();
        NotificationException[] failedAttempts = new NotificationException[failures];
        Arrays.fill(failedAttempts, new NotificationException("Send failed"));
        doThrow(failedAttempts).doNothing().when(spyUser).sendNotification(anyString());

        // Act + Assert
        assertDoesNotThrow(() -> library.notifyUserWithBookReviews(validInputIsbn, validInputUserId));
        verify(spyUser, times(failures + 1)).sendNotification(expectedNotificationMessage);
    }

    @Test
    @Timeout(1) // guards against an endless retry loop
    void GivenNotificationAlwaysFails_WhenNotifyUserWithBookReviews_ThenThrowNotificationExceptionAfterFiveAttempts() {
        // Arrange
        arrangeValidBookUserAndReviews();
        doThrow(new NotificationException("Send failed")).when(spyUser).sendNotification(anyString());

        // Act
        NotificationException exception = assertThrows(NotificationException.class,
                () -> library.notifyUserWithBookReviews(validInputIsbn, validInputUserId));

        // Assert - exactly 5 attempts, no more and no less
        assertEquals("Notification failed!", exception.getMessage());
        verify(spyUser, times(5)).sendNotification(expectedNotificationMessage);
    }

    @Test
    @Timeout(1) // guards against an endless retry loop
    void GivenNotificationAlwaysFails_WhenNotifyUserWithBookReviews_ThenLogEveryRetryAttempt() {
        // Arrange - every send fails, and System.err is redirected so the log can be read
        arrangeValidBookUserAndReviews();
        doThrow(new NotificationException("Send failed")).when(spyUser).sendNotification(anyString());
        PrintStream originalErr = System.err;
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(capturedErr));

        // Act
        try {
            assertThrows(NotificationException.class,
                    () -> library.notifyUserWithBookReviews(validInputIsbn, validInputUserId));
        } finally {
            System.setErr(originalErr); // restore even if the assertion fails
        }

        // Assert - one numbered line per failed attempt
        String newLine = System.lineSeparator();
        String expectedLog =
                "Notification failed! Retrying attempt 1/5" + newLine +
                "Notification failed! Retrying attempt 2/5" + newLine +
                "Notification failed! Retrying attempt 3/5" + newLine +
                "Notification failed! Retrying attempt 4/5" + newLine +
                "Notification failed! Retrying attempt 5/5" + newLine;
        assertEquals(expectedLog, capturedErr.toString());
    }


    // ===================== getBookByISBN =====================


    @Test
    void GivenValidFreeBookAndUser_WhenGetBookByISBN_ThenReturnBookAndNotifyUser() {
        // Arrange
        arrangeValidBookUserAndReviews();

        // Act
        Book result = library.getBookByISBN(validInputIsbn, validInputUserId);

        // Assert
        assertSame(spyBook, result);
        verify(mockDatabaseService, times(2)).getBookByISBN(validInputIsbn);
        verify(mockDatabaseService, times(1)).getUserById(validInputUserId);
        verify(mockReviewService, times(1)).getReviewsForBook(validInputIsbn);
        verify(mockReviewService, times(1)).close();
        verify(spyUser, times(1)).sendNotification(expectedNotificationMessage);
    }

    //Invalid ISBN
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "978030640615",       // 12 digits
            "97803064061570",     // 14 digits
            "978030640615X",      // contains a letter
            "978030640615 ",      // contains a space
            "9780306406158",      // wrong check digit
            "978-0-306-40615-8"  // hyphenated, wrong check digit
    })
    void GivenInvalidIsbn_WhenGetBookByISBN_ThenThrowIllegalArgumentException(String invalidIsbn) {
        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.getBookByISBN(invalidIsbn, validInputUserId));

        // Assert
        assertEquals("Invalid ISBN.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService, mockReviewService);
    }

    //Invalid UserId
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "12345678901",       // 11 digits
            "1234567890123",     // 13 digits
            "12345678901a",      // contains a letter
            " 12345678901",      // contains a space
            "1234-5678901"       // contains a hyphen
    })
    void GivenInvalidUserId_WhenGetBookByISBN_ThenThrowIllegalArgumentException(String invalidUserId) {
        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.getBookByISBN(validInputIsbn, invalidUserId));

        // Assert
        assertEquals("Invalid user Id.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService, mockReviewService);
    }


    @Test
    void GivenBookNotInDatabase_WhenGetBookByISBN_ThenThrowBookNotFoundException() {
        // Arrange
        when(mockDatabaseService.getBookByISBN(validInputIsbn)).thenReturn(null);

        // Act
        BookNotFoundException exception = assertThrows(BookNotFoundException.class,
                () -> library.getBookByISBN(validInputIsbn, validInputUserId));

        // Assert
        assertEquals("Book not found!", exception.getMessage());
        verify(mockDatabaseService, times(1)).getBookByISBN(validInputIsbn);
        verifyNoInteractions(mockReviewService);
        verifyNoInteractions(mockNotificationService);
    }


    @Test
    void GivenBorrowedBook_WhenGetBookByISBN_ThenThrowBookAlreadyBorrowedException() {
        // Arrange
        doReturn(true).when(spyBook).isBorrowed();
        when(mockDatabaseService.getBookByISBN(validInputIsbn)).thenReturn(spyBook);

        // Act
        BookAlreadyBorrowedException exception = assertThrows(BookAlreadyBorrowedException.class,
                () -> library.getBookByISBN(validInputIsbn, validInputUserId));

        // Assert
        assertEquals("Book was already borrowed!", exception.getMessage());
        verify(mockDatabaseService, times(1)).getBookByISBN(validInputIsbn);
        verifyNoInteractions(mockReviewService, mockNotificationService);
    }



    @Test
    void GivenNotificationFails_WhenGetBookByISBN_ThenReturnBook() {
        // Arrange
        arrangeValidBookUserAndReviews();
        doThrow(new NotificationException("Send failed")).when(spyUser).sendNotification(anyString());

        // Act
        Book result = assertDoesNotThrow(() -> library.getBookByISBN(validInputIsbn, validInputUserId));

        // Assert
        assertSame(spyBook, result);
        verify(spyUser, times(5)).sendNotification(expectedNotificationMessage);
        verify(mockReviewService, times(1)).close();
    }



    @Test
    void GivenUserNotRegistered_WhenGetBookByISBN_ThenReturnBook() {
        // Arrange
        when(mockDatabaseService.getBookByISBN(validInputIsbn)).thenReturn(spyBook);
        when(mockDatabaseService.getUserById(validInputUserId)).thenReturn(null);

        // Act
        Book result = assertDoesNotThrow(
                () -> library.getBookByISBN(validInputIsbn, validInputUserId));

        // Assert
        assertSame(spyBook, result);
        verify(mockDatabaseService, times(1)).getUserById(validInputUserId);
        verifyNoInteractions(mockReviewService, mockNotificationService);
    }



    @Test
    void GivenReviewServiceFails_WhenGetBookByISBN_ThenReturnBook() {
        // Arrange
        arrangeValidBookUserAndReviews();
        when(mockReviewService.getReviewsForBook(validInputIsbn))
                .thenThrow(new ReviewException("Connection lost"));

        // Act
        Book result = assertDoesNotThrow(
                () -> library.getBookByISBN(validInputIsbn, validInputUserId));

        // Assert
        assertSame(spyBook, result);
        verify(mockReviewService, times(1)).getReviewsForBook(validInputIsbn);
        verify(mockReviewService, times(1)).close();
        verifyNoInteractions(mockNotificationService);
    }



    // ===================== ReturnBook =====================

    @ParameterizedTest
    @ValueSource(strings = {
            "978-0-306-40615-7",
            "9780306406157",
            "9783161484100"
    })
    void GivenValidBorrowedBook_WhenReturnBook_ThenBookIsReturned(String validIsbn) {
        // Arrange
        spyBook.borrow();
        when(mockDatabaseService.getBookByISBN(validIsbn)).thenReturn(spyBook);

        // Act
        library.returnBook(validIsbn);

        // Assert
        assertFalse(spyBook.isBorrowed());
        verify(spyBook, times(1)).returnBook();
        verify(mockDatabaseService, times(1)).getBookByISBN(validIsbn);
        verify(spyBook, times(1)).returnBook();
        verify(mockDatabaseService, times(1)).returnBook(validIsbn);
    }


    @Test
    void GivenBookNotInDatabase_WhenReturnBook_ThenThrowBookNotFoundException() {
        // Arrange
        when(mockDatabaseService.getBookByISBN(validInputIsbn)).thenReturn(null);

        // Act
        BookNotFoundException exception = assertThrows(BookNotFoundException.class,
                () -> library.returnBook(validInputIsbn));

        // Assert
        assertEquals("Book not found!", exception.getMessage());
        verify(mockDatabaseService, times(1)).getBookByISBN(validInputIsbn);
        verify(mockDatabaseService, never()).returnBook(anyString());
    }


    @Test
    void GivenBookNotBorrowed_WhenReturnBook_ThenThrowBookNotBorrowedException() {
        // Arrange
        when(mockDatabaseService.getBookByISBN(validInputIsbn)).thenReturn(spyBook);

        // Act
        BookNotBorrowedException exception = assertThrows(BookNotBorrowedException.class,
                () -> library.returnBook(validInputIsbn));

        // Assert
        assertEquals("Book wasn't borrowed!", exception.getMessage());
        verify(mockDatabaseService, times(1)).getBookByISBN(validInputIsbn);
        verify(spyBook, never()).returnBook();
        verify(mockDatabaseService, never()).returnBook(anyString());
    }


    // ===================== BorrowBook =====================
    @ParameterizedTest
    @CsvSource({
            "978-0-306-40615-7, 123456789012",
            "9780306406157, 000000000001",
            "9783161484100, 000000000000"
    })
    void GivenValidUserAndAvailableBook_WhenBorrowBook_ThenBookIsBorrowed(
            String validIsbn, String validUserId) {

        // Arrange
        when(mockDatabaseService.getBookByISBN(validIsbn)).thenReturn(spyBook);
        when(mockDatabaseService.getUserById(validUserId)).thenReturn(spyUser);

        // Act
        library.borrowBook(validIsbn, validUserId);

        // Assert
        assertTrue(spyBook.isBorrowed());
        verify(spyBook, times(1)).borrow();
        verify(mockDatabaseService, times(1)).getBookByISBN(validIsbn);
        verify(mockDatabaseService, times(1)).getUserById(validUserId);
        verify(mockDatabaseService, times(1)).borrowBook(validIsbn, validUserId);
    }


    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "978030640615",       // 12 digits
            "97803064061570",     // 14 digits
            "978030640615X",      // contains a letter
            "978030640615 ",      // contains a space
            "9780306406158",      // wrong check digit
            "978-0-306-40615-8"  // hyphenated, wrong check digit
    })
    void GivenInvalidIsbn_WhenBorrowBook_ThenThrowIllegalArgumentException(String invalidIsbn) {
        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.borrowBook(invalidIsbn, validInputUserId));

        // Assert
        assertEquals("Invalid ISBN.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
        verify(spyBook, never()).borrow();
    }



    @Test
    void GivenBookNotInDatabase_WhenBorrowBook_ThenThrowBookNotFoundException() {
        // Arrange
        when(mockDatabaseService.getBookByISBN(validInputIsbn)).thenReturn(null);

        // Act
        BookNotFoundException exception = assertThrows(BookNotFoundException.class,
                () -> library.borrowBook(validInputIsbn, validInputUserId));

        // Assert
        assertEquals("Book not found!", exception.getMessage());
        verify(mockDatabaseService, times(1)).getBookByISBN(validInputIsbn);
        verify(mockDatabaseService, never()).getUserById(anyString());
        verify(mockDatabaseService, never()).borrowBook(anyString(), anyString());
    }



    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "12345678901",       // 11 digits
            "1234567890123",     // 13 digits
            "12345678901a",      // contains a letter
            " 12345678901",      // contains a space
            "1234-5678901"       // contains a hyphen
    })
    void GivenInvalidUserId_WhenBorrowBook_ThenThrowIllegalArgumentException(String invalidUserId) {
        // Arrange
        when(mockDatabaseService.getBookByISBN(validInputIsbn)).thenReturn(spyBook);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.borrowBook(validInputIsbn, invalidUserId));

        // Assert
        assertEquals("Invalid user Id.", exception.getMessage());
        verify(mockDatabaseService, times(1)).getBookByISBN(validInputIsbn);
        verify(mockDatabaseService, never()).getUserById(anyString());
        verify(mockDatabaseService, never()).borrowBook(anyString(), anyString());
        verify(spyBook, never()).borrow();
    }



    @Test
    void GivenUserNotInDatabase_WhenBorrowBook_ThenThrowUserNotRegisteredException() {
        // Arrange
        when(mockDatabaseService.getBookByISBN(validInputIsbn)).thenReturn(spyBook);
        when(mockDatabaseService.getUserById(validInputUserId)).thenReturn(null);

        // Act
        UserNotRegisteredException exception = assertThrows(UserNotRegisteredException.class,
                () -> library.borrowBook(validInputIsbn, validInputUserId));

        // Assert
        assertEquals("User not found!", exception.getMessage());
        verify(mockDatabaseService, times(1)).getBookByISBN(validInputIsbn);
        verify(mockDatabaseService, times(1)).getUserById(validInputUserId);
        verify(spyBook, never()).borrow();
        verify(mockDatabaseService, never()).borrowBook(anyString(), anyString());
    }



    @Test
    void GivenAlreadyBorrowedBook_WhenBorrowBook_ThenThrowBookAlreadyBorrowedException() {
        // Arrange
        spyBook.borrow();
        clearInvocations(spyBook);
        when(mockDatabaseService.getBookByISBN(validInputIsbn)).thenReturn(spyBook);
        when(mockDatabaseService.getUserById(validInputUserId)).thenReturn(spyUser);

        // Act
        BookAlreadyBorrowedException exception = assertThrows(BookAlreadyBorrowedException.class,
                () -> library.borrowBook(validInputIsbn, validInputUserId));

        // Assert
        assertEquals("Book is already borrowed!", exception.getMessage());
        verify(mockDatabaseService, times(1)).getBookByISBN(validInputIsbn);
        verify(mockDatabaseService, times(1)).getUserById(validInputUserId);
        verify(spyBook, times(1)).isBorrowed();
        verify(spyBook, never()).borrow();
        verify(mockDatabaseService, never()).borrowBook(anyString(), anyString());
    }







}