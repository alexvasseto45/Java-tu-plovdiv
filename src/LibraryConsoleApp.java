import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import java.util.Scanner;
import org.bson.types.ObjectId;
import java.util.List;
import java.util.ArrayList;



public class LibraryConsoleApp {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final Scanner scanner = new Scanner(System.in);
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> usersCollection;
    private static MongoCollection<Document> librariesCollection;
    private static MongoCollection<Document> bookRatingsCollection;
    private static MongoCollection<Document> userBooksCollection;
    private static MongoCollection<Document> booksCollection;
    private static String libraryId;

    public static void main(String[] args) {
        mongoClient = MongoClients.create(CONNECTION_STRING);
        database = mongoClient.getDatabase("LibrarySystem");
        usersCollection = database.getCollection("users");
        librariesCollection = database.getCollection("libraries");
        bookRatingsCollection = database.getCollection("book_ratings");
        userBooksCollection = database.getCollection("user_books");
        booksCollection = database.getCollection("books");

        chooseLibrary();
        mainMenu();
    }

    private static void chooseLibrary() {
        while (true) {
            System.out.println("\nChoose a Library:");
            FindIterable<Document> libraries = librariesCollection.find();
            int index = 1;
            for (Document lib : libraries) {
                System.out.println(index + ". " + lib.getString("name"));
                index++;
            }
            System.out.println(index + ". Add New Library");
            System.out.print("Choose an option: ");

            int choice = Integer.parseInt(scanner.nextLine());
            if (choice > 0 && choice < index) {
                Document selectedLibrary = libraries.skip(choice - 1).first();
                libraryId = selectedLibrary.getObjectId("_id").toString();
                System.out.println("You selected: " + selectedLibrary.getString("name"));
                break;
            } else if (choice == index) {
                addNewLibrary();
            } else {
                System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private static void addNewLibrary() {
        System.out.print("Enter new library name: ");
        String libraryName = scanner.nextLine();

        Document existingLibrary = librariesCollection.find(Filters.eq("name", libraryName)).first();
        if (existingLibrary != null) {
            System.out.println("Library already exists.");
            return;
        }

        Document newLibrary = new Document("name", libraryName);
        librariesCollection.insertOne(newLibrary);
        libraryId = newLibrary.getObjectId("_id").toString();
        System.out.println("Library added successfully!");
    }

    private static void mainMenu() {
        while (true) {
            System.out.println("\n1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    login();
                    break;
                case "2":
                    register();
                    break;
                case "3":
                    System.exit(0);
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }
    private static String loggedInUser;
    private static String loggedInUserId;
    private static String userRole;

    private static void login() {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        Document user = usersCollection.find(Filters.and(
                Filters.eq("username", username),
                Filters.eq("password", password),
                Filters.eq("library_id", libraryId)
        )).first();

        if (user != null) {
            loggedInUser = username;
            loggedInUserId = user.getObjectId("_id").toString();
            //System.out.print(loggedInUserId);
            userRole = user.getString("role");
            System.out.println("Login successful! Welcome " + username);
            userMenu();
        } else {
            System.out.println("Invalid credentials or library.");
        }
    }

    private static void register() {
        System.out.print("Enter new username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        System.out.print("Enter role (admin/user): ");
        String role = scanner.nextLine().trim().toLowerCase();

        if (!role.equals("admin") && !role.equals("user")) {
            System.out.println("Invalid role. Please enter 'admin' or 'user'.");
            return;
        }

        Document existingUser = usersCollection.find(Filters.and(
                Filters.eq("username", username),
                Filters.eq("library_id", libraryId)
        )).first();

        if (existingUser != null) {
            System.out.println("Username already exists in this library.");
            return;
        }

        Document newUser = new Document("username", username)
                .append("password", password)
                .append("library_id", libraryId)
                .append("role", role);

        usersCollection.insertOne(newUser);
        System.out.println("Registration successful!");
    }

    //should test with normal user 5.Cancel
    private static void userMenu() {
        while (true) {
            System.out.println("\n1. My Books");
            System.out.println("2. Whole Collection");

            if (userRole.trim().equalsIgnoreCase("admin")) {
                System.out.println("3. Add a Book");
                System.out.println("4. Delete a Book");
            }
            System.out.println("5. Logout");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    showUserBooks();
                    break;
                case "2":
                    showBooksInLibrary();
                    break;
                case "3":
                    if (userRole.trim().equalsIgnoreCase("admin")) addBook();
                    else System.out.println("Invalid option.");
                    break;
                case "4":
                    if (userRole.trim().equalsIgnoreCase("admin")) deleteBook();
                    else System.out.println("Invalid option.");
                    break;
                case "5":
                    loggedInUser = null;
                    userRole = null;
                    return;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }

    private static int readIntInRange(int min, int max) {
        while (true) {
            try {
                int input = Integer.parseInt(scanner.nextLine());
                if (input >= min && input <= max) return input;
            } catch (NumberFormatException ignored) {}
            System.out.print("Invalid input. Try again: ");
        }
    }

    private static void showUserBooks() {
        FindIterable<Document> userBooks = userBooksCollection.find(Filters.eq("owner", loggedInUserId));
        List<Document> bookList = new ArrayList<>();

        System.out.println("\nYour Books:");
        int index = 1;
        for (Document userBook : userBooks) {
            Document book = booksCollection.find(Filters.eq("_id", userBook.getObjectId("book_id"))).first();
            if (book != null) {
                System.out.println(index + ". " + book.getString("title"));
                bookList.add(userBook);
                index++;
            }
        }

        if (bookList.isEmpty()) {
            System.out.println("You have no books in your collection.");
            return;
        }

        System.out.print("Select a book by number (or 0 to go back): ");
        int choice = readIntInRange(0, bookList.size());
        if (choice == 0) return;

        Document selectedUserBook = bookList.get(choice - 1);
        bookOptionsMenu(selectedUserBook);
    }

    private static void bookOptionsMenu(Document userBook) {
        ObjectId book_id = userBook.getObjectId("book_id");
        System.out.println(userBook.getString("owner"));
        Document book = booksCollection.find(Filters.eq("_id", book_id)).first();
        Document user = usersCollection.find(Filters.eq("_id", new ObjectId(userBook.getString("owner")))).first();

        if (book == null || user == null) {
            System.out.println("No book or user found");
            return;
        }
        String bookTitle = book.getString("title");
        String userName = user.getString("username");
        while (true) {
            System.out.println("\nSelected Book: " + book.getString("title"));
            System.out.println("1. Rate book");
            System.out.println("2. Remove book from account");
            System.out.println("0. Go back");
            System.out.print("Choose option: ");
            int option = readIntInRange(0, 2);

            switch (option) {
                case 1 -> rateBook(bookTitle, book_id);
                case 2 -> {
                    userBooksCollection.deleteOne(Filters.eq("_id", userBook.getObjectId("_id")));
                    System.out.println("Book removed.");
                    return;
                }
                case 0 -> { return; }
            }
        }
    }

    private static void rateBook(String bookName, ObjectId book_id) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Rate the book (1-5): ");
        int rating = scanner.nextInt();
        scanner.nextLine();

        Document ratingDoc = new Document("book_name", bookName)
                .append("book_id", book_id)
                .append("rating", rating)
                .append("from_user", loggedInUserId);

        bookRatingsCollection.insertOne(ratingDoc);

        System.out.println("Thanks for rating!");
    }


    private static void addBook() {
        MongoCollection<Document> booksCollection = database.getCollection("books");

        System.out.print("Enter book title: ");
        String title = scanner.nextLine();

        Document newBook = new Document("title", title)
                .append("library_id", libraryId)
                .append("owner", null);

        booksCollection.insertOne(newBook);
        System.out.println("Book added successfully!");
    }

    private static void deleteBook() {
        MongoCollection<Document> booksCollection = database.getCollection("books");

        System.out.print("Enter book title to delete: ");
        String title = scanner.nextLine();

        Document book = booksCollection.find(Filters.and(
                Filters.eq("title", title),
                Filters.eq("library_id", libraryId)
        )).first();

        if (book != null) {
            booksCollection.deleteOne(Filters.eq("_id", book.getObjectId("_id")));
            System.out.println("Book deleted successfully!");
        } else {
            System.out.println("Book not found.");
        }
    }

    private static void showBooksInLibrary() {
        System.out.println("\nAll Books in Library:");
        FindIterable<Document> books = booksCollection.find(Filters.eq("library_id", libraryId));

        int index = 1;
        for (Document book : books) {
            float rating = find_avg_rating(book);
            //System.out.println(index + ". " + book.getString("title"));
            System.out.printf("%d. %s (Avg Rating: %.2f)%n", index, book.getString("title"), rating);
            index++;
        }

        System.out.println(index + ". Cancel");
        System.out.print("Choose a book to add to your collection: ");
        int choice = Integer.parseInt(scanner.nextLine());

        if (choice == index) {
            return;
        }

        Document selectedBook = books.skip(choice - 1).first();
        if (selectedBook != null) {
            //System.out.println("DEBUG: Selected Book - " + selectedBook.getString("title"));

            MongoCollection<Document> userBooksCollection = database.getCollection("user_books");

            Document newUserBook = new Document("book_id", selectedBook.getObjectId("_id"))
                    .append("owner", loggedInUserId)
                    .append("rating", -1);

            //System.out.println("DEBUG: New User Book Document - " + newUserBook.toJson());

            userBooksCollection.insertOne(newUserBook);
            System.out.println("Book added to your collection successfully!");
        } else {
            System.out.println("Invalid choice.");
        }
    }

    private static float find_avg_rating(Document book) {
        ObjectId bookId = book.getObjectId("_id");

        FindIterable<Document> ratings = bookRatingsCollection.find(Filters.eq("book_id", bookId));

        int totalRating = 0;
        int count = 0;

        for (Document ratingDoc : ratings) {
            int rating = ratingDoc.getInteger("rating", -1);
            if (rating >= 1 && rating <= 5) {
                totalRating += rating;
                count++;
            }
        }

        if (count == 0) return 0.0f;
        return (float) totalRating / count;
    }


}
