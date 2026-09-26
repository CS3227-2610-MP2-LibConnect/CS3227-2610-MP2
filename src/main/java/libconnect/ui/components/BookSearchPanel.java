package libconnect.ui.components;

import java.util.Objects;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import libconnect.services.BookSearchCriteria;

/** Provides simple and advanced catalogue search controls. */
public final class BookSearchPanel extends VBox {
    private static final String SIMPLE_SEARCH = "Simple search";
    private static final String ADVANCED_SEARCH = "Advanced search";

    private final Consumer<BookSearchCriteria> searchHandler;
    private final Consumer<String> errorHandler;
    private final ComboBox<String> searchTypeSelector;
    private final TextField simpleTitleField;
    private final TextField isbnField;
    private final TextField titleField;
    private final TextField authorField;
    private final TextField publisherField;
    private final TextField categoryField;
    private final TextField publicationYearStartField;
    private final TextField publicationYearEndField;
    private final VBox simpleSearchFields;
    private final GridPane advancedSearchFields;

    /**
     * Creates a search panel that reports valid criteria and validation errors to the supplied handlers.
     *
     * @param searchHandler the action invoked with valid search criteria.
     * @param errorHandler the action invoked with a validation error message.
     */
    public BookSearchPanel(Consumer<BookSearchCriteria> searchHandler,
                           Consumer<String> errorHandler) {
        this.searchHandler = Objects.requireNonNull(searchHandler, "searchHandler");
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
        searchTypeSelector = new ComboBox<>();
        simpleTitleField = new TextField();
        isbnField = new TextField();
        titleField = new TextField();
        authorField = new TextField();
        publisherField = new TextField();
        categoryField = new TextField();
        publicationYearStartField = new TextField();
        publicationYearEndField = new TextField();
        simpleSearchFields = createSimpleSearchFields();
        advancedSearchFields = createAdvancedSearchFields();

        searchTypeSelector.getItems().addAll(SIMPLE_SEARCH, ADVANCED_SEARCH);
        searchTypeSelector.setValue(SIMPLE_SEARCH);
        searchTypeSelector.setMaxWidth(Double.MAX_VALUE);
        searchTypeSelector.valueProperty().addListener(
                (observable, oldValue, newValue) -> updateSearchFields(newValue));

        setSpacing(10);
        getChildren().addAll(new Label("Search type"), searchTypeSelector,
                simpleSearchFields, advancedSearchFields);
        updateSearchFields(SIMPLE_SEARCH);
    }

    /**
     * Creates the simple title search controls.
     *
     * @return the simple search controls.
     */
    private VBox createSimpleSearchFields() {
        simpleTitleField.setPromptText("Search by title");
        simpleTitleField.setOnAction(event -> performSearch());

        VBox fields = new VBox(6, simpleTitleField, createSearchActions());
        fields.setPadding(new Insets(0, 0, 4, 0));
        return fields;
    }

    /**
     * Creates the advanced search controls for all searchable book fields.
     *
     * @return the advanced search controls.
     */
    private GridPane createAdvancedSearchFields() {
        isbnField.setPromptText("Exact ISBN");
        titleField.setPromptText("Title contains");
        authorField.setPromptText("Author contains");
        publisherField.setPromptText("Publisher contains");
        categoryField.setPromptText("Category contains");
        publicationYearStartField.setPromptText("Start year");
        publicationYearEndField.setPromptText("End year");

        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(8);
        addAdvancedField(fields, "ISBN", isbnField, 0);
        addAdvancedField(fields, "Title", titleField, 1);
        addAdvancedField(fields, "Author", authorField, 2);
        addAdvancedField(fields, "Publisher", publisherField, 3);
        addAdvancedField(fields, "Category", categoryField, 4);
        addAdvancedField(fields, "Publication year from", publicationYearStartField, 5);
        addAdvancedField(fields, "Publication year to", publicationYearEndField, 6);

        fields.add(createSearchActions(), 1, 7);
        return fields;
    }

    /**
     * Creates the search and reset actions used by both search modes.
     *
     * @return the search action controls.
     */
    private HBox createSearchActions() {
        Button searchButton = new Button("Search");
        searchButton.setOnAction(event -> performSearch());

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(event -> resetFilters());

        return new HBox(8, searchButton, resetButton);
    }

    /**
     * Adds one labelled advanced-search field to the grid.
     *
     * @param fields the grid receiving the field.
     * @param labelText the field label.
     * @param field the input field.
     * @param row the target row.
     */
    private void addAdvancedField(GridPane fields, String labelText, TextField field, int row) {
        fields.add(new Label(labelText), 0, row);
        fields.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
        field.setMaxWidth(Double.MAX_VALUE);
    }

    /**
     * Shows the controls for the selected search type.
     *
     * @param searchType the selected search type.
     */
    private void updateSearchFields(String searchType) {
        boolean isSimpleSearch = SIMPLE_SEARCH.equals(searchType);
        simpleSearchFields.setVisible(isSimpleSearch);
        simpleSearchFields.setManaged(isSimpleSearch);
        advancedSearchFields.setVisible(!isSimpleSearch);
        advancedSearchFields.setManaged(!isSimpleSearch);
    }

    /**
     * Validates the active controls and invokes the search handler.
     */
    private void performSearch() {
        try {
            BookSearchCriteria criteria = SIMPLE_SEARCH.equals(searchTypeSelector.getValue())
                    ? new BookSearchCriteria(null, simpleTitleField.getText(), null, null, null, null, null)
                    : createAdvancedCriteria();
            errorHandler.accept(null);
            searchHandler.accept(criteria);
        } catch (IllegalArgumentException exception) {
            errorHandler.accept(exception.getMessage());
        }
    }

    /** Clears all search fields and restores the complete book list. */
    private void resetFilters() {
        simpleTitleField.clear();
        isbnField.clear();
        titleField.clear();
        authorField.clear();
        publisherField.clear();
        categoryField.clear();
        publicationYearStartField.clear();
        publicationYearEndField.clear();
        performSearch();
    }

    /**
     * Creates criteria from the advanced search fields.
     *
     * @return the advanced search criteria.
     * @throws IllegalArgumentException if either publication year is not a positive integer,
     *                                  or if the range is reversed.
     */
    private BookSearchCriteria createAdvancedCriteria() {
        Integer publicationYearStart = parsePublicationYear(publicationYearStartField.getText(), "Start year");
        Integer publicationYearEnd = parsePublicationYear(publicationYearEndField.getText(), "End year");
        return new BookSearchCriteria(isbnField.getText(), titleField.getText(),
                authorField.getText(), publisherField.getText(), categoryField.getText(),
                publicationYearStart, publicationYearEnd);
    }

    /**
     * Parses an optional publication year input.
     *
     * @param text the input text.
     * @param fieldName the display name of the year field.
     * @return the parsed year, or null when the input is blank.
     * @throws IllegalArgumentException if the input is not a positive integer.
     */
    private Integer parsePublicationYear(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            return null;
        }

        try {
            int publicationYear = Integer.parseInt(text.trim());
            if (publicationYear <= 0) {
                throw new IllegalArgumentException(fieldName + " must be positive.");
            }
            return publicationYear;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be a number.");
        }
    }
}
