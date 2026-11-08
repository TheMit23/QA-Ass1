package ac.il.bgu.qa;

import ac.il.bgu.qa.errors.*;
import ac.il.bgu.qa.services.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;

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

    @BeforeEach 
    void setup(){
        MockitoAnnotations.openMocks(this);
        library = new Library(mockDatabaseService, mockReviewService);
        spyBook = spy(new Book(validInputIsbn, validInputTitle, validInputAuthor));
        spyUser = spy(new User(validInputUserName, validInputUserId, mockNotificationService));
    }
}