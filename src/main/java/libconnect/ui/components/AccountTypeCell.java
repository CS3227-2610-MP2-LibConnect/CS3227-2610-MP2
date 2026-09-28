package libconnect.ui.components;

import javafx.scene.control.ListCell;

import libconnect.models.AccountType;

/** Renders account types with user-facing labels in selector controls. */
public final class AccountTypeCell extends ListCell<AccountType> {
    /**
     * Updates the displayed label for the current account type.
     *
     * @param accountType the account type represented by this cell.
     * @param empty whether the cell does not represent an item.
     */
    @Override
    protected void updateItem(AccountType accountType, boolean empty) {
        super.updateItem(accountType, empty);
        setText(empty || accountType == null ? null
                : accountType == AccountType.LIBRARIAN ? "Librarian" : "Member");
    }
}
