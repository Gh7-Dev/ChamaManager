package chamamanager.presentation;

import chamamanager.exceptions.DataAccessException;
import chamamanager.exceptions.DuplicateContributionException;
import chamamanager.model.Contribution;
import chamamanager.logic.ContributionService;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;

/** Screen 1: record a contribution. Talks to ContributionService only. */
public class RecordContributionPanel extends JFrame {

    private final ContributionService service = new ContributionService();
    private final JTextField memberIdField = new JTextField(15);
    private final JTextField amountField = new JTextField(15);
    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 15);
    private final JSpinner periodPicker = new JSpinner(new SpinnerDateModel());

    public RecordContributionPanel() {
        super("Record Contribution");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Month picker: shows and edits yyyy-MM only
        periodPicker.setEditor(new JSpinner.DateEditor(periodPicker, "yyyy-MM"));
        periodPicker.setValue(new Date());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.anchor = GridBagConstraints.WEST;

        addRow(form, g, 0, "Member ID:", memberIdField);
        addRow(form, g, 1, "Amount:", amountField);
        addRow(form, g, 2, "Date paid (yyyy-MM-dd):", dateField);
        addRow(form, g, 3, "Period (YYYY-MM):", periodPicker);

        JButton saveBtn = new JButton("Submit for Approval");
        JButton clearBtn = new JButton("Clear");
        saveBtn.addActionListener(e -> save());
        clearBtn.addActionListener(e -> clear());

        JPanel buttons = new JPanel();
        buttons.add(saveBtn);
        buttons.add(clearBtn);

        add(new JLabel("New contributions stay PENDING until the Treasurer approves them.",
                SwingConstants.CENTER), BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private void addRow(JPanel p, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridx = 0; g.gridy = row;
        p.add(new JLabel(label), g);
        g.gridx = 1;
        p.add(field, g);
    }

    /** Reads the picker as YYYY-MM, e.g. 2026-10. */
    private String selectedPeriod() {
        Date d = (Date) periodPicker.getValue();
        return YearMonth.from(d.toInstant().atZone(ZoneId.systemDefault())).toString();
    }

    private void save() {
        try {
            int memberId = Integer.parseInt(memberIdField.getText().trim());
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            Contribution c = new Contribution(memberId, amount, date, selectedPeriod());

            if (service.record(c)) {
                JOptionPane.showMessageDialog(this, "Saved. Status: PENDING Treasurer approval.");
                clear();
            }
        } catch (NumberFormatException ex) {
            error("Member ID and amount must be valid numbers.");
        } catch (DateTimeParseException ex) {
            error("Date must look like 2026-10-10.");
        } catch (DuplicateContributionException ex) {
            error(ex.getMessage());
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        } catch (DataAccessException ex) {
            error("Database error: " + ex.getMessage());
        }
    }

    private void clear() {
        memberIdField.setText("");
        amountField.setText("");
        dateField.setText(LocalDate.now().toString());
        periodPicker.setValue(new Date());
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}