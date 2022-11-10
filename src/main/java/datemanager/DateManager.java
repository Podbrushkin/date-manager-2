package datemanager;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.filechooser.*;
import javax.swing.*;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Image;
import java.awt.TrayIcon;
import java.awt.SystemTray;
import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowStateListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.ItemEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

// import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.ResourceBundle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Files;

public class DateManager extends JFrame {
	// private static ResourceBundle rb = ResourceBundle.getBundle("localization", Locale.getDefault());
	// private static ResourceBundle rb = ResourceBundle.getBundle("localization", Locale.forLanguageTag("ru"));
	private static ResourceBundle rb;
	private JTable table1 = null;
	private File dirForData = new File(System.getProperty("user.home")+File.separator+".napominalka");
	private File defaultDataFile = new File(dirForData, "data.tsv");
	private File settingsFile = new File(dirForData, "settings.tsv");
	private Map<String,String> settings = readSettingsFromFile(settingsFile.toPath());
	File autostartDir = new File(System.getProperty("user.home") + 
					"\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup");
	private static boolean startHidden = false;
	
	private List<String> possibleFormatsString = List.of("yyyy-MM-dd","d MMMM y", "yyyy MMM dd", "dd MMM yyyy", 
														"yyyy MM dd", "dd-MM-yyyy","dd MM yyyy");
	// private ArrayList<DateTimeFormatter> possibleFormats = generatePossibleFormats();
	private Map<String,DateTimeFormatter> possibleFormats = generatePossibleFormats();
	DateTimeFormatter customDateFormatter = null;
	
	public DateManager() {
		super("Date Manager");
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.out.println(e.getComponent());
				// System.out.println(e.getSource());
				// System.out.println(e.getWindow());
				var dimension = e.getWindow().getSize();
				settings.put("jframe.preferredWidth", ""+(int)dimension.getWidth());
				settings.put("jframe.preferredHeight", ""+(int)dimension.getHeight());
				
				var location = e.getWindow().getLocation();
				settings.put("jframe.x", ""+(int)location.getX());
				settings.put("jframe.y", ""+(int)location.getY());
			}
		});
		
		
		Runtime.getRuntime().addShutdownHook(new Thread(()-> {
			saveSettings();
		}));
		
		String customDatePattern = settings.getOrDefault("dateFormat","yyyy-MM-dd");
		customDateFormatter = new DateTimeFormatterBuilder()
                               .parseCaseInsensitive()
                               .appendPattern(customDatePattern)
                               .toFormatter()
							   .withLocale(Locale.forLanguageTag("ru-RU"));
		
		
		
		
		table1 = new JTable(new MyTableModel()) {
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component c = super.prepareRenderer(renderer, row, column);

				//  Alternate row color

				if (!isRowSelected(row)) {
					var tableModel = ((MyTableModel)table1.getModel());
					var ld = (LocalDate)tableModel.getValueAt(convertRowIndexToModel​(row), 0);
					
					if (ld.equals(LocalDate.now()))
						c.setBackground(new Color(140,240,140));
					else if (ld.equals(tableModel.getClosestDateInFuture()))
						c.setBackground(Color.LIGHT_GRAY);
					else 
						c.setBackground(getBackground());
				}
				return c;
			}
		};
		
		var columnNames = new String[]{rb.getString("dateColumnName"), rb.getString("descColumnName")};
		((MyTableModel)table1.getModel()).setColumnNames(columnNames);
		table1.getTableHeader().getColumnModel().getColumn(0).setHeaderValue(columnNames[0]);
		table1.getTableHeader().getColumnModel().getColumn(1).setHeaderValue(columnNames[1]);
		
		System.out.println("font size initial:"+table1.getFont().getSize());
		
		table1.getTableHeader().setReorderingAllowed(false);
		var localDateCellEditor = new LocalDateCellEditor(new ArrayList<>(possibleFormats.values()));
		table1.getColumnModel().getColumn(0).setCellEditor(localDateCellEditor);
		table1.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JTextField()) {
			@Override
			public Component getTableCellEditorComponent(JTable table, 
					Object value, boolean isSelected, int row, int column)  {
				
				JTextField textField = (JTextField) super.getComponent();
				
				System.out.println("table1.getFont()="+table1.getFont());
				textField.setFont(table1.getFont());
				
				// textField.setText(((LocalDate) value).toString());
				textField.setText(value.toString());
				return textField; 
			} 
		});
		
		
		
		
		table1.getColumnModel().getColumn(0).addPropertyChangeListener((evt) -> {
			var propertyName = evt.getPropertyName();
			if (propertyName.equals("preferredWidth")) {	// || propertyName.equals("width")
				System.out.println(propertyName+"="+evt.getNewValue());
				settings.put("dateColumn."+propertyName, evt.getNewValue().toString());
			}
		});
		table1.getColumnModel().getColumn(1).addPropertyChangeListener((evt) -> {
			var propertyName = evt.getPropertyName();
			if (propertyName.equals("preferredWidth")) {	// || propertyName.equals("width")
				System.out.println(propertyName+"="+evt.getNewValue());
				settings.put("descColumn."+propertyName, evt.getNewValue().toString());
			}
		});
		
		
		var tableRowSorter = new TableRowSorter<>(table1.getModel());
		tableRowSorter.setComparator(0, new MyDateWoYearComparator());
		tableRowSorter.setComparator(1, Comparator.naturalOrder());
		table1.setRowSorter(tableRowSorter);
		
		List<RowSorter.SortKey> sortKeys = new ArrayList<>();
 
		int columnIndexToSort = 0;
		sortKeys.add(new RowSorter.SortKey(columnIndexToSort, SortOrder.ASCENDING));
		 
		tableRowSorter.setSortKeys(sortKeys);
		tableRowSorter.sort();
		
		enableDeleteKey(table1);
		
		var localDateRenderer = new LocalDateCellRenderer(customDateFormatter);
		localDateRenderer.setHorizontalAlignment( SwingConstants.CENTER );
		table1.getColumnModel().getColumn(0).setCellRenderer( localDateRenderer );
		
		importFromFile(defaultDataFile.toPath());
		
		tableViewLoadSettings();
		
		final JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem(rb.getString("context.delete"));
        deleteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
				deleteSelectedRows(table1);
            }
        });
        popupMenu.add(deleteItem);
        table1.setComponentPopupMenu(popupMenu);
		makeRightClickSelectRow(popupMenu);
		
		
		var menuBar = new JMenuBar();
		menuBar.add(createFileMenu());
		menuBar.add(createEditMenu());
		menuBar.add(createViewMenu());
		menuBar.add(createHelpMenu());
		setJMenuBar(menuBar);
		
		
		
		addIconToTrayAndWindow();
		
		var contents = new JPanel();
		contents.setLayout(new GridLayout(1,1,20,20));
		contents.add(new JScrollPane(table1));
		
		
		setContentPane(contents);
		var preferredWidth = settings.get("jframe.preferredWidth");
		var preferredHeight = settings.get("jframe.preferredHeight");
		if (preferredWidth != null && preferredHeight != null)
			setSize(Integer.parseInt(preferredWidth), Integer.parseInt(preferredHeight));
		else setSize(this.getPreferredSize());
		
		var x = settings.get("jframe.x");
		var y = settings.get("jframe.y");
		if (x != null && y != null)
			setLocation(Integer.parseInt(x), Integer.parseInt(y));
		else
			setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		if (!startHidden) setVisible(true);
		/* System.out.println("superAfter");
		System.out.println("getPreferredWidth="+table1.getColumnModel().getColumn(0).getPreferredWidth());
		System.out.println("getWidth="+table1.getColumnModel().getColumn(0).getWidth());
		table1.getColumnModel().getColumn(1).setPreferredWidth(200); */
	}
	
	private JMenu createFileMenu() {
		var menu = new JMenu(rb.getString("menu.file"));
		
		var btn = new JMenuItem(rb.getString("menu.file.save"));
		btn.addActionListener((ae) -> {
			var tmp = 
			settings.entrySet().stream()
				.map(e -> new String[]{e.getKey(), e.getValue()})
				.toList();
			if (tmp.size() > 0)
				writeToFile(tmp, settingsFile.toPath());
			saveChanges(defaultDataFile.toPath());
		});
		// btn.setToolTipText(""+defaultDataFile+".");
		btn.setToolTipText(String.format(rb.getString("menu.file.save_"),defaultDataFile));
		menu.add(btn);
		
		btn = new JMenuItem(rb.getString("menu.file.revert"));
		btn.addActionListener((ae) -> {
			var tableModel = ((MyTableModel)table1.getModel());
			tableModel.removeRange(0, tableModel.getRowCount()-1);
			importFromFile(defaultDataFile.toPath());
		});
		// btn.setToolTipText("<html>Discard all changes, reset to last saved state. All records "+
			// "in table will be deleted and <br/>those from "+defaultDataFile+
			// " will be added instead</html>");
		btn.setToolTipText(String.format(rb.getString("menu.file.revert_"),defaultDataFile));
		menu.add(btn);
		
		btn = new JMenuItem(rb.getString("menu.file.import"));
		btn.addActionListener((ae) -> {
			var fileChooser = new JFileChooser();
			FileFilter type1 = new FileNameExtensionFilter("Text files", "txt", "tsv");
			fileChooser.addChoosableFileFilter(type1);
			int response = fileChooser.showOpenDialog(menu);
			if (response == JFileChooser.APPROVE_OPTION) {
				importFromFile(fileChooser.getSelectedFile().toPath());
			}
		});
		// btn.setToolTipText("Add new records from file");
		btn.setToolTipText(rb.getString("menu.file.import_"));
		menu.add(btn);
		
		btn = new JMenuItem(rb.getString("menu.file.export"));
		btn.addActionListener((ae) -> {
			var fileChooser = new JFileChooser();
			FileFilter type1 = new FileNameExtensionFilter("TSV file", ".tsv");
			fileChooser.addChoosableFileFilter(type1);
			fileChooser.setSelectedFile(new File("dates_"+LocalDate.now()+".tsv"));
			int response = fileChooser.showSaveDialog(menu);
			if (response == JFileChooser.APPROVE_OPTION) {
				saveChanges(fileChooser.getSelectedFile().toPath());
			}
		});
		// btn.setToolTipText("Save all records to external file");
		btn.setToolTipText(rb.getString("menu.file.export_"));
		menu.add(btn);
		
		String curJar = "";
		try {
			curJar = 
			this.getClass().getProtectionDomain()
				.getCodeSource().getLocation()
				.toURI().getPath().toLowerCase();
		} catch (Exception e) {
			System.err.println("Failed to determine where app is started from, "+
				"autostart will not be available");
		}
		
		if (System.getProperty("os.name").contains("Windows") &&
			curJar.endsWith(".jar")) {
				
			btn = createAutostartItem();
			menu.add(btn);
		}
		
		btn = new JMenuItem(rb.getString("menu.file.exit"));
		btn.addActionListener((ae) -> { 
			WindowEvent windowClosing = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
			this.dispatchEvent(windowClosing);
			
			// this.dispose(); System.exit(0); 
		});
		btn.setToolTipText("Exit without saving current state, all changes will be lost");
		btn.setToolTipText(rb.getString("menu.file.exit_"));
		menu.add(btn);
		return menu;
	}
	
	private JMenu createViewMenu() {
		var menu = new JMenu(rb.getString("menu.view"));
		var menu2 = new JMenu(rb.getString("menu.view.dateFormat"));
		menu2.setToolTipText(rb.getString("menu.view.dateFormat_"));
		ButtonGroup bg = new ButtonGroup();
		for (var format : possibleFormats.entrySet()) {
			var btn = new JRadioButtonMenuItem(LocalDate.now().format(format.getValue()));
			btn.addActionListener((ae) -> {
				var localDateRenderer = new LocalDateCellRenderer(format.getValue());
				table1.getColumnModel().getColumn(0).setCellRenderer( localDateRenderer );
				table1.repaint();
				settings.put("dateFormat", format.getKey());
			});
			
			if (format.getValue().toString().equals(customDateFormatter.toString())) {
				btn.setSelected(true);
			}
			btn.setToolTipText(format.getKey());
			bg.add(btn);
			menu2.add(btn);
		}
		// menu2.setToolTipText("Change display format. It doesn't affect data files");
		menu.add(menu2);
		
		
		menu2 = new JMenu(rb.getString("menu.view.dateSort"));
		menu2.setToolTipText(rb.getString("menu.view.dateSort_"));
		bg = new ButtonGroup();
		
		var btn = new JRadioButtonMenuItem(rb.getString("menu.view.dateSortWithYear"));
		btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
				((DefaultRowSorter)table1.getRowSorter()).setComparator(0, Comparator.naturalOrder());
				settings.put("dateSorting", "withYear");
				((TableRowSorter)table1.getRowSorter()).sort();
            }
        });
		// System.out.println("Comparator:");
		// System.out.println(((DefaultRowSorter)table1.getRowSorter()).getComparator(0));
		if (((DefaultRowSorter)table1.getRowSorter()).getComparator(0).equals(Comparator.naturalOrder()))
			btn.setSelected(true);
		// btn.setToolTipText("Dates are arranged in absolute order");
		btn.setToolTipText(rb.getString("menu.view.dateSortWithYear_"));
		bg.add(btn);
		menu2.add(btn);
		
		btn = new JRadioButtonMenuItem(rb.getString("menu.view.dateSortWithoutYear"));
		btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
				((DefaultRowSorter)table1.getRowSorter()).setComparator(0, new MyDateWoYearComparator());
				settings.put("dateSorting", "withoutYear");
				((TableRowSorter)table1.getRowSorter()).sort();
			}
        });
		// System.out.println("Comparator:");
		// System.out.println(((DefaultRowSorter)table1.getRowSorter()).getComparator(0));
		if (((DefaultRowSorter)table1.getRowSorter()).getComparator(0).equals(new MyDateWoYearComparator()))
			btn.setSelected(true);
		// btn.setToolTipText("Sort like all dates are in the range of one year");
		btn.setToolTipText(rb.getString("menu.view.dateSortWithoutYear_"));
		bg.add(btn);
		menu2.add(btn);
		menu.add(menu2);
		
		menu2 = new JMenu(rb.getString("menu.view.uiScaling"));
		menu2.setToolTipText(rb.getString("menu.view.uiScaling_"));
		bg = new ButtonGroup();
		
		var set = new TreeSet<Double>();
		for (double i = 1; i < 5; i+=0.5) {
			set.add(i);
		}
		for (var i : set) {
			btn = new JRadioButtonMenuItem(""+i);
			btn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					settings.put("uiScale", ""+i);
					
					showRestartNeededWindow();
					
				}
			});
			// System.out.println("Comparator:");
			// System.out.println(((DefaultRowSorter)table1.getRowSorter()).getComparator(0));
			// System.out.println("sun.java2d.uiScale="+System.getProperty("sun.java2d.uiScale"));
			var tmp = (""+i);	//.replaceAll("\\.0+","");
			if (System.getProperty("sun.java2d.uiScale").equals(tmp))
				btn.setSelected(true);
			bg.add(btn);
			menu2.add(btn);
		}
		menu.add(menu2);
		
		
		menu2 = new JMenu(rb.getString("menu.view.tableFontScale"));
		menu2.setToolTipText(rb.getString("menu.view.tableFontScale_"));
		bg = new ButtonGroup();
		
		set = new TreeSet<Double>();
		for (double i = 06; i <= 20; i+=02) {
			set.add(i/10);
		}
		for (var i : set) {
			btn = new JRadioButtonMenuItem(""+i);
			btn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// var oldFont = table1.getFont();
					var oldFont = (Font) UIManager.get("Table.font");
					var newFont = oldFont.deriveFont((float)(oldFont.getSize()*i));
					table1.setFont(newFont);
					// table1.setRowHeight((int)(newFont.getSize()*1.2));
					settings.put("tableFontScale", ""+i);
					
					var tmp = Double.parseDouble(settings.getOrDefault("tableRowHeight", "1.0"));
					var newSize = table1.getFont().getSize()*1.2 * tmp;
					table1.setRowHeight((int)newSize);
				}
			});
			var fontScaleStr = settings.get("tableFontScale");
			if (fontScaleStr == null && i == 1.0)
				btn.setSelected(true);
			else if (fontScaleStr != null && fontScaleStr.equals(i+""))
				btn.setSelected(true);
			bg.add(btn);
			menu2.add(btn);
		}
		menu.add(menu2);
		
		
		menu2 = new JMenu(rb.getString("menu.view.tableRowHeight"));
		menu2.setToolTipText(rb.getString("menu.view.tableRowHeight_"));
		bg = new ButtonGroup();
		System.out.println("getRowHeight:"+table1.getRowHeight());
		// final double originalRowHeight = table1.getRowHeight();
		set = new TreeSet<Double>();
		for (double i = 06; i <= 18; i+=02) {
			set.add(i/10);
		}
		for (var i : set) {
			btn = new JRadioButtonMenuItem(""+i);
			btn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					var oldFont = table1.getFont();
					// var oldFont = (Font) UIManager.get("Table.font");
					// System.out.println(i +" "+oldFont.getSize()*1.2*i);
					System.out.println("getRowHeight was:"+table1.getRowHeight());
					
					var newSize = table1.getFont().getSize()*1.2 * i;
					table1.setRowHeight((int)newSize);
					// table1.setRowHeight((int)Math.ceil(((double)oldFont.getSize())*1.2*i));
					
					System.out.println("getRowHeight now:"+table1.getRowHeight());
					// table1.setRowHeight((int)Math.ceil(originalRowHeight * i));
					settings.put("tableRowHeight", ""+i);
				}
			});
			var tableRowHeight = settings.get("tableRowHeight");
			if (tableRowHeight == null && i == 1.0)
				btn.setSelected(true);
			else if (tableRowHeight != null && tableRowHeight.equals(i+""))
				btn.setSelected(true);
			bg.add(btn);
			menu2.add(btn);
		}
		menu.add(menu2);
		
		menu2 = new JMenu("Language");
		menu2.setText(rb.getString("menu.view.language"));
		menu2.setToolTipText(rb.getString("menu.view.language_"));
		bg = new ButtonGroup();
		btn = new JRadioButtonMenuItem("English");
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				settings.put("language", "en");
				showRestartNeededWindow();
			}
		});
		bg.add(btn);
		menu2.add(btn);
		btn.setSelected(true);
		
		
		btn = new JRadioButtonMenuItem("Russian");
		btn.setText(rb.getString("menu.view.language.ru"));
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				settings.put("language", "ru");
				showRestartNeededWindow();
			}
		});
		bg.add(btn);
		menu2.add(btn);
		if (settings.get("language") != null && settings.get("language").equals("ru"))
			btn.setSelected(true);
		
		menu.add(menu2);
		
		return menu;
	}
	
	private JMenu createEditMenu() {
		var menu = new JMenu(rb.getString("menu.edit"));
		
		var btn = new JMenuItem(rb.getString("menu.edit.insert"));
		btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((MyTableModel)table1.getModel()).newRow();
            }
        });
		// btn.setToolTipText("Add new record with today date");
		btn.setToolTipText(rb.getString("menu.edit.insert_"));
		menu.add(btn);
		
		btn = new JMenuItem(rb.getString("menu.edit.delete"));
		btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
				deleteSelectedRows(table1);
            }
        });
		btn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		// btn.setToolTipText("Delete selected rows");
		btn.setToolTipText(rb.getString("menu.edit.delete_"));
		menu.add(btn);
		
		btn = new JMenuItem(rb.getString("menu.edit.deleteDuplicates"));
		btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((MyTableModel)table1.getModel()).removeDuplicates();
            }
        });
		btn.setToolTipText(rb.getString("menu.edit.deleteDuplicates_"));
		// btn.setToolTipText("If there are records with the same date and description, "+
							// "leave only one of them");
		menu.add(btn);
		
		btn = new JMenuItem(rb.getString("menu.edit.deleteAll"));
		btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
				var tableModel = ((MyTableModel)table1.getModel());
                tableModel.removeRange(0, tableModel.getRowCount()-1);
            }
        });
		// btn.setToolTipText("Clear table");
		btn.setToolTipText(rb.getString("menu.edit.deleteAll_"));
		menu.add(btn);
		return menu;
	}
	
	private JMenu createHelpMenu() {
		var menu = new JMenu(rb.getString("menu.help"));
		
		var btn = new JMenuItem(rb.getString("menu.help.openAppDir"));
		btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
				try {
					File curJar = 
						new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
                
					Desktop.getDesktop().open(curJar);
				} catch (Exception ex) {ex.printStackTrace();}
            }
        });
		// btn.setToolTipText("Open folder which contains this app's executable");
		btn.setToolTipText(rb.getString("menu.help.openAppDir_"));
		menu.add(btn);
		
		btn = new JMenuItem(rb.getString("menu.help.openDataDir"));
		btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
				try {
					Desktop.getDesktop().open(dirForData);
				} catch (Exception ex) {ex.printStackTrace();}
            }
        });
		// btn.setToolTipText("Open folder which contains this app's data");
		btn.setToolTipText(rb.getString("menu.help.openDataDir_"));
		menu.add(btn);
		
		if (System.getProperty("os.name").contains("Windows")) {
		btn = new JMenuItem(rb.getString("menu.help.openAutostartDir"));
		btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
				try {
					Desktop.getDesktop().open(autostartDir);
				} catch (Exception ex) {ex.printStackTrace();}
            }
        });
		// btn.setToolTipText("Open folder where this app creates it's shortcut to be run on startup");
		btn.setToolTipText(rb.getString("menu.help.openAutostartDir_"));
		menu.add(btn);
		}
		
		return menu;
	}
	
	private void updateRowHeights(JTable table) {
		for (int row = 0; row < table.getRowCount(); row++) {
			int rowHeight = table.getRowHeight();

			for (int column = 0; column < table.getColumnCount(); column++)
			{
				Component comp = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
				rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
			}

			table.setRowHeight(row, rowHeight);
		}
	}
	
	private void enableDeleteKey(JTable table) {
		InputMap inputMap = table.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap actionMap = table.getActionMap();

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		actionMap.put("delete", new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				deleteSelectedRows(table);
				
				/* var simpleRange = true;
				for (int i = 1; i < rows.length; i++) {
					if (rows[i] - 1 != rows[i-1]) simpleRange = false;
				}
				if (simpleRange) 
					tableModel.removeRange(rows[0], rows[rows.length-1]);
				else {
					if (rows.length > 0) {
						for (int i = rows.length - 1; i >= 0; i--) {
							tableModel.removeRange(rows[i], rows[i]);
						}
					}
				} */
				
					/* if ((from == -1) && (i < rows.length)) { 
						from = i;
						System.out.println("from set to "+i);
						continue;
					}
					
					if ((i == rows.length) || (rows[i] - 1 != rows[i-1])) {
						System.out.println("i="+i);
						tableModel.removeRange(rows[from], rows[i-1]);
						from = -1;
					} */
				
				// if (simpleRange) {
				// tableModel.removeRange(rows[0], rows[rows.length-1]);

				// for (int i = 0; i < rows.length; i++) {
					// int row = table.convertRowIndexToModel(rows[i]);
				// }

				// if (row >= 0 && col >= 0) {
					// row = table.convertRowIndexToModel(row);
					// col = table.convertColumnIndexToModel(col);
					// table.getModel().setValueAt(null, row, col);
				// }
			}
		});
	}
	
	private void deleteSelectedRows(JTable table) {
		int[] rows = table.getSelectedRows();

		for (int i = 0; i < rows.length; i++) {
			rows[i] = table.convertRowIndexToModel(rows[i]);
		}
		Arrays.sort(rows);
		System.out.println(Arrays.toString(rows));
		var tableModel = (MyTableModel)table1.getModel();
		
		int to = rows.length - 1;
		
		for (int i = rows.length -2; i >= -1; i--) {
			if (i == -1) {
				tableModel.removeRange(rows[i+1], rows[to]);
			} else 
			if (rows[i] != rows[i+1] -1) {
				tableModel.removeRange(rows[i+1], rows[to]);
				to = i;
			}
		}
	}
	
	private void importFromFile(Path file) {
		try {
			table1.getSelectionModel().clearSelection();
			var tableModel = (MyTableModel)table1.getModel();
			int rowsBefore = tableModel.getRowCount();
			Files.lines(file)
				.filter(s -> s.matches("\\d.*\\t.*"))
				.map(s -> s.split("\t"))
				.forEach(arr -> tableModel.newRow(LocalDate.parse(arr[0]), arr[1]));
			
			if (!file.equals(defaultDataFile.toPath()))
			for (int i = rowsBefore; i < tableModel.getRowCount(); i++) {
				int tmp = table1.convertRowIndexToView(i);
				table1.getSelectionModel().addSelectionInterval(tmp, tmp);
			}
			
			
		} catch (Exception e) {e.printStackTrace();}
	}
	
	private void writeToFile(List<String[]> data, Path file) {
		try (var pw = new PrintWriter(file.toFile(), "utf-8")) {
			for (var pair : data) {
				pw.println(pair[0]+"\t"+pair[1]);
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Ошибка записи в файл через PrintWriter: "+file+"\t"+e,
				"Error",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void saveChanges(Path file) {
		var tableModel = (MyTableModel)table1.getModel();
		var data = new ArrayList<String[]>();
		for (int i = 0; i < tableModel.getRowCount(); i++) {
			var date = tableModel.getValueAt(i, 0);
			var desc = tableModel.getValueAt(i, 1);
			data.add(new String[]{date.toString(), desc.toString()});
		}
		writeToFile(data, file);
		
	}
	
	private static Map<String,String> readSettingsFromFile(Path settingsFile) {
		var settings = new HashMap<String,String>();
		if (Files.exists(settingsFile))
		try {
			Files.lines(settingsFile)
				.filter(s -> s.matches("\\w.*\\t.*"))
				.map(s -> s.split("\t"))
				.forEach(arr -> settings.put(arr[0], arr[1]));
		} catch (Exception e) {e.printStackTrace();}
		return settings;
	}
	
	private void saveSettings() {
		var tmp = 
		settings.entrySet().stream()
			.map(e -> new String[]{e.getKey(), e.getValue()})
			.toList();
		if (tmp.size() > 0)
			writeToFile(tmp, settingsFile.toPath());
	}
	
	private void makeRightClickSelectRow(JPopupMenu popupMenu) {
		popupMenu.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        int rowAtPoint = table1.rowAtPoint(SwingUtilities.convertPoint(popupMenu, new Point(0, 0), table1));
                        if ((rowAtPoint > -1) && (table1.getSelectedRows().length < 2)) {
                            table1.setRowSelectionInterval(rowAtPoint, rowAtPoint);
                        }
                    }
                });
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                // TODO Auto-generated method stub

            }
        });
	}
	
	private Map<String,DateTimeFormatter> generatePossibleFormats() {
		Map<String,DateTimeFormatter> possibleFormats = new LinkedHashMap<>();
		for (var s : possibleFormatsString) {
			
			var format1 = new DateTimeFormatterBuilder()
                               .parseCaseInsensitive()
                               .appendPattern(s)
                               .toFormatter();
			
		   
			var format2 = format1;
			if (s.contains("MMM"))
				format2 = format1.withLocale(Locale.forLanguageTag("ru-RU"));
			
			// var format1 = DateTimeFormatter.ofPattern(s);
			// var format2 = DateTimeFormatter.ofPattern(s, new Locale("ru"));
			
			
			possibleFormats.put(s, format2);
			// System.out.println(s+" = \t"+LocalDate.now().format(format1));
			// if (!format1.equals(format2)) {
				// System.out.println(s+" = \t"+LocalDate.now().format(format2));
				// possibleFormats.add(format2);
			// }
		}
		return possibleFormats;
	}
	
	private void addIconToTrayAndWindow() {
		
		TrayIcon trayIcon = null;
		if (SystemTray.isSupported()) {
			Image image = null;
			try {
				image = new ImageIcon(this.getClass().getClassLoader().getResourceAsStream("birthdayCakeIcon.png").readAllBytes()).getImage();
			} catch (Exception e) {System.out.println("Failed to load icon:"+e);}
			this.setIconImage(image);
			SystemTray tray = SystemTray.getSystemTray();
			
			
			trayIcon = new TrayIcon(image, rb.getString("appTitle"), null);
			trayIcon.setImageAutoSize(true);
			
			// trayIcon.setToolTip(String.format("Ближайшая дата: %s %s", entry.getKey(), entry.getValue()));
			
			JFrame[] frameTmp = new JFrame[]{this};
			trayIcon.addMouseListener(new MouseListener() {
				public void mouseClicked(MouseEvent e) {
					if (true || e.getButton()==1) {
						JFrame frame = frameTmp[0];
						if (frame.isVisible()) frame.setVisible(false);
						else {
							frame.setVisible(true);
							frame.setExtendedState(JFrame.NORMAL);
							frame.setAlwaysOnTop(true);
							frame.setAlwaysOnTop(false);
							
						}
					}
				}
				public void mouseEntered(MouseEvent e) {}
				public void mouseExited(MouseEvent e) {}
				public void mousePressed(MouseEvent e) {}
				public void mouseReleased(MouseEvent e) {}
			});
			try {
				tray.add(trayIcon);
			} catch (Exception e) {
				System.out.println("Failed to add TrayIcon to Tray"+e);
			}
			
			this.addWindowStateListener(new WindowStateListener() {
				public void windowStateChanged(WindowEvent we) {
					JFrame frame = frameTmp[0];
					if (we.getNewState() == JFrame.ICONIFIED) {
						frame.setVisible(false);
						frame.setExtendedState(JFrame.NORMAL);
					} else
					if (we.getNewState() == WindowEvent.WINDOW_CLOSING) {
						
					}
				}
			});
			
		}
		
	}
	
	private JCheckBoxMenuItem createAutostartItem() {
		var autostartItem = new JCheckBoxMenuItem(rb.getString("menu.file.autostart"));
		
		String shortcuts = Arrays.stream(autostartDir.listFiles())
				.filter(f -> f.getName().toLowerCase().contains("napominalka"))
				.map(Object::toString).collect(Collectors.joining(" "));
		
		if (shortcuts.toLowerCase().contains("napominalka")) {
			System.out.printf("App's link(s) detected in autostart: %s%n", shortcuts);
			autostartItem.setState(true);
		} else autostartItem.setState(false);
		
		
		autostartItem.addItemListener((ie) -> {
			Path link = autostartDir.toPath().resolve("Napominalka.lnk");
			if (ie.getStateChange() == ItemEvent.SELECTED) {
				try {
					Path curJar = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toPath();
					System.out.println(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
					System.out.println("curJar: "+curJar);
					if (!curJar.toString().endsWith(".jar")) return;
					if (Files.exists(link)) {
						System.out.printf("Attempt to create duplicating autostart link - checkbox is in wrong state");
						return;
					}
					
					// var pw = new java.io.PrintWriter(link.toFile(), "866");
					// String jarParam = (curJar.toString().endsWith(".jar")) ? " -jar" : "";
					// String command = "start javaw"+jarParam+" \""+curJar.toAbsolutePath().toString()+"\" --hidden >nul\n";
					// pw.write(command);
					// pw.flush(); pw.close();
					
					// Files.createSymbolicLink(link, curJar);
					ShortcutFactory.createShortcut(curJar, link);
					System.out.printf("Autostart link created: pathToApp=[%s], link=[%s]%n",curJar,link);
					
				} catch (Exception e) {
					System.out.printf("Failed to add app to autostart %s%n",e);
				}
			}
			else if (ie.getStateChange() == ItemEvent.DESELECTED) {
				if (Files.exists(link)) 
					try { 
						Files.delete(link);
						System.out.printf("Autostart link deleted: %s%n",link);
					} catch (Exception e) {
						System.out.printf("Failed to remove app from autostart %s%n",e);
					}
				else System.out.printf("Attempt to remove unexisting link!%n");
				
			}
		});
		// autostartItem.setToolTipText("<html>Creates a link in "+autostartDir+".<br/>"+
		// "You can create it by yourself, filename should contain 'napominalka'</html>");
		autostartItem.setToolTipText(String.format(rb.getString("menu.file.autostart_"),autostartDir));
		return autostartItem;
	}
	
	private void tableViewLoadSettings() {
		
		
		if (settings.get("dateSorting") != null) {
			var tableRowSorter = new TableRowSorter<>(table1.getModel());
			switch (settings.get("dateSorting")) {
				case "withYear" : tableRowSorter.setComparator(0, Comparator.naturalOrder()); break;
				case "withoutYear" : tableRowSorter.setComparator(0, new MyDateWoYearComparator()); break;
				default : tableRowSorter.setComparator(0, new MyDateWoYearComparator());
			}
			tableRowSorter.setComparator(1, Comparator.naturalOrder());
			table1.setRowSorter(tableRowSorter);
		}
		
		
		var fontScaleStr = settings.get("tableFontScale");
		if (fontScaleStr != null) {
			System.out.println("font size was:"+table1.getFont().getSize());
			var newSize = table1.getFont().getSize() * Float.parseFloat(fontScaleStr);
			var newFont = table1.getFont().deriveFont(newSize);
			table1.setFont(newFont);
			System.out.println("font size now:"+table1.getFont().getSize());
			// var cellEditor = table1.getColumnModel().getColumn(1).getCellEditor();
			// System.out.println("cellEditor.getClass()="+cellEditor.getClass());
			// cellEditor.getComponent().setFont(newFont);
			// var tmp = new JTextField();
			// tmp.setFont(newFont);
			// table1.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(tmp));
		}
		var tableRowHeight = settings.get("tableRowHeight");
		if (tableRowHeight != null) {
			System.out.println("font size was:"+table1.getFont().getSize());
			System.out.println("getRowHeight was:"+table1.getRowHeight());
			
			var newSize = table1.getFont().getSize()*1.2 * Float.parseFloat(tableRowHeight);
			table1.setRowHeight((int)newSize);
			
			System.out.println("getRowHeight now:"+table1.getRowHeight());
			System.out.println("font size now:"+table1.getFont().getSize());
		}
		
		var preferredWidth = settings.get("dateColumn.preferredWidth");
		if (preferredWidth != null) {
			var newSize = Integer.parseInt(preferredWidth);
			table1.getColumnModel().getColumn(0).setPreferredWidth(newSize);
			// System.out.printf("descColumnWidth is set to %s and now is %s%n",newSize,
				// table1.getColumnModel().getColumn(1).getPreferredWidth());
		}
		preferredWidth = settings.get("descColumn.preferredWidth");
		if (preferredWidth != null) {
			var newSize = Integer.parseInt(preferredWidth);
			table1.getColumnModel().getColumn(1).setPreferredWidth(newSize);
			// System.out.printf("descColumnWidth is set to %s and now is %s%n",newSize,
				// table1.getColumnModel().getColumn(1).getPreferredWidth());
		}
	}
	
	private void showRestartNeededWindow() {
		JOptionPane jop = new JOptionPane();
		jop.setMessageType(JOptionPane.INFORMATION_MESSAGE);
		// jop.setMessage("Restart application to make changes visible");
		jop.setMessage(rb.getString("window.restartNeeded"));
		// JDialog dialog = jop.createDialog(null, "Information");
		JDialog dialog = jop.createDialog(null, rb.getString("window.restartNeededTitle"));

		// Set a 2 second timer
		/* new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(2000);
				} catch (Exception e) { }
				dialog.dispose();
			}
		}).start(); */

		dialog.setVisible(true);
	}
	
	public static void main(String[] args) {
		var tmp = new File(System.getProperty("user.home")+File.separator+".napominalka"+File.separator+"settings.tsv");
		var settings = DateManager.readSettingsFromFile(tmp.toPath());
		System.setProperty("sun.java2d.uiScale", settings.getOrDefault("uiScale", "3.0"));
		var languageTag = settings.getOrDefault("language", "en");
		rb = ResourceBundle.getBundle("localization", Locale.forLanguageTag(languageTag));
		
		if (Set.of(args).contains("--hidden")) startHidden = true;
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel");
			// UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception e) {e.printStackTrace();}
		new DateManager();
	}
	
	private static void printAvailableLAFs() {
      UIManager.LookAndFeelInfo info[] = UIManager.getInstalledLookAndFeels();
      for( int i = 0; i < info.length; i++ ) {
         UIManager.LookAndFeelInfo lookAndFeelInfo = info[i];
         System.out.println( lookAndFeelInfo );
      }
   }

	private static void printActionMap(JComponent component) {
		InputMap inputMap = null;
		inputMap = component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		// if (component instanceof JTable)
			// inputMap = (InputMap) UIManager.get("Table.ancestorInputMap");
		var actionMap = component.getActionMap();
		var keys = inputMap.allKeys();
		Arrays.sort(keys, Comparator.comparing(o -> o.toString()));
		for (var key : keys) {
			System.out.printf("%s = %s\n", key, inputMap.get(key));
		}
		
		System.out.println("-->>");
		var tmp = ((InputMap) UIManager.get("Table.ancestorInputMap")).get(KeyStroke.getKeyStroke((char)127));
		System.out.println(tmp);
		
		KeyEventPostProcessor kepp = new KeyEventPostProcessor() {
		  @Override 
		  public boolean postProcessKeyEvent(KeyEvent e) {
			// if (e.getKeyChar() == 127)
				// System.out.println(e);
				// System.out.println(e.getComponent());
				// System.out.println(e.getSource());
			// int c = e.getKeyChar();
			// System.out.println(c);
			return false;
		  }
		};
		KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		kfm.addKeyEventPostProcessor(kepp);
		
	}

class LocalDateCellEditor extends DefaultCellEditor { //implements TableCellEditor 
	// public DateTimeFormatter customDateFormatter;
    // private List<DateTimeFormatter> possibleFormats = null;
    
	
	

    // private JTextField textField = new JTextField();

    public LocalDateCellEditor(List<DateTimeFormatter> possibleFormats) {
		super(new JTextField());
		
		// this.possibleFormats = possibleFormats;
		// this.customDateFormatter = possibleFormats.get(0);
		
		
        ((JTextField) super.getComponent()).setHorizontalAlignment(SwingConstants.CENTER);
        ((JTextField) super.getComponent()).setFont(table1.getFont());
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, 
            Object value, boolean isSelected, int row, int column)  {
		
		JTextField textField = (JTextField) super.getComponent();
		
		System.out.println("table1.getFont()="+table1.getFont());
		textField.setFont(table1.getFont());
		
		// textField.setText(((LocalDate) value).toString());
		textField.setText(((LocalDate) value).format(customDateFormatter));
        return textField; 
    } 


    @Override
    public Object getCellEditorValue() { 
        return LocalDate.parse(((JTextField) super.getComponent()).getText(), customDateFormatter); 
    }
	
	@Override
    public boolean stopCellEditing() {
		JTextField textField = (JTextField) super.getComponent();
		String s = (String) super.getCellEditorValue();
		LocalDate ld = null;
		for (var format : possibleFormats.values()) {
			
			try {
				ld = LocalDate.parse(s, format);
			} catch (Exception e) {}
			
			if (ld != null) {
				textField.setText(ld.format(customDateFormatter));
				return super.stopCellEditing();
			}
		}
		
		
		textField.setBorder(new LineBorder(Color.red));
		return false;
		
		/* try {
			var ld = LocalDate.parse(s, customDateFormatter);
			System.out.println(s);
		} catch (Exception e) {
			JTextField textField = (JTextField) super.getComponent();
			textField.setBorder(new LineBorder(Color.red));
			return false;
		}
		return super.stopCellEditing(); */
	}
}

}

class MyTableModel extends AbstractTableModel {
	
	private String[] columnNames = {"Date", "Description"};
	/* private Object[][] data = {
		{LocalDate.now(), "Kathy Smith"},
		{LocalDate.now(), "Pity Fella"},
	}; */
	
	// private ArrayList<Map.Entry<LocalDate,String>> data = new ArrayList<>();
	
	private ArrayList<Object[]> data = new ArrayList<>();
	private LocalDate closestDateInFuture = null;
	
	MyTableModel() {
		// data.add(new Object[]{LocalDate.now(), "Kathy Smith"});
		// data.add(new Object[]{LocalDate.now(), "Pity Fella"});
	}

	public void setColumnNames(String[] columnNames) {
		// System.out.println("setColumnNames:"+Arrays.toString(columnNames));
		this.columnNames = columnNames;
	}
	
	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return data.size();
	}

	public String getColumnName(int col) {
		// System.out.println(Arrays.toString(columnNames));
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		// System.out.printf("(%s,%s): %s\n",row,col,data.get(row)[col]);
		return data.get(row)[col];
	}
	
	public Class getColumnClass(int c) {
		
		switch (c) {
			case 0 : return LocalDate.class;
			case 1 : return String.class;
			default : return null;
		}
		
		// return getValueAt(0, c).getClass();
	}

	/*
	 * Don't need to implement this method unless your table's
	 * editable.
	 */
	public boolean isCellEditable(int row, int col) {
		//Note that the data/cell address is constant,
		//no matter where the cell appears onscreen.
		if (col < 2) {
			return true;
		} else {
			return true;
		}
	}

	/*
	 * Don't need to implement this method unless your table's
	 * data can change.
	 */
	boolean DEBUG = false;
	public void setValueAt(Object value, int row, int col) {
		if (DEBUG) {
			System.out.println("Setting value at " + row + "," + col
							   + " to " + value
							   + " (an instance of "
							   + value.getClass() + ")");
		}

		data.get(row)[col] = value;
		setClosestDateInFuture();
		fireTableCellUpdated(row, col);

		if (DEBUG) {
			System.out.println("New value of data:");
			printDebugData();
		}
	}
	
	public void newRow() {
		newRow(LocalDate.now(), "Description");
	}
	public void newRow(LocalDate date, String desc) {
		data.add(new Object[]{date, desc});
		// fireTableCellUpdated(data.size()-1, 1);
		setClosestDateInFuture();
		fireTableRowsInserted(data.size() - 1, data.size() - 1);
	}
	
	// inclusive, inclusive
	public void removeRange(int from, int to) {
		if (from > to) return;
		System.out.printf("Removing range: %s-%s\n",from,to);
		for (int i = from; i <= to; i++) {
			System.out.printf("Removing this: %s %s\n", getValueAt(i,0),getValueAt(i,1));
		}
			
		data.subList(from, to+1).clear();
		setClosestDateInFuture();
		fireTableRowsDeleted(from, to);
	}
	
	public void removeDuplicates() {
		// LinkedHashSet<Object[]> tmp = new LinkedHashSet<Object[]>();
		System.out.println("before:"+data.size());
		// tmp.addAll(data);
		// System.out.println("set:"+tmp.size());
		var tmp = new ArrayList<Object[]>();
		for (int i = 0; i < data.size(); i++) {
			var record1 = data.get(i);
			for (int j = 0; j < data.size(); j++) {
				var record2 = data.get(j);
				if ((i != j) && (record1[0].equals(record2[0])) && (record1[1].equals(record2[1]))) {
					data.remove(record1);
					break;
				}
			}
		}
		
		System.out.println("after:"+data.size());
		// data.clear();
		// data.addAll(tmp);
		fireTableDataChanged();
	}
	
	public LocalDate getClosestDateInFuture() {
		return closestDateInFuture;
	}
	
	private void setClosestDateInFuture() {
		// dataHashCode = data.hashCode();
		var today = LocalDate.now();
		var currentYear = today.getYear();
		
		var tmpSet = new TreeSet<LocalDate>(new MyDateWoYearComparator());
		tmpSet.addAll(data.stream().map(a -> (LocalDate)a[0]).toList());
		closestDateInFuture = tmpSet.higher(today);
		if (closestDateInFuture == null)
		for (var ld : tmpSet) {
			System.out.print(".");
			if (ld.withYear(currentYear).isAfter(today)) {
				closestDateInFuture = ld;
				return;
			}
		}
		// closestDateInFuture = (LocalDate)data.get(0)[0];
		// closestDateInFuture = tmpSet.first();
	}
	
	private void printDebugData() {
		int numRows = getRowCount();
		int numCols = getColumnCount();

		for (int i=0; i < numRows; i++) {
			System.out.print("    row " + i + ":");
			for (int j=0; j < numCols; j++) {
				System.out.print("  " + data.get(i)[j]);
			}
			System.out.println();
		}
		System.out.println("--------------------------");
	}
}



class LocalDateCellRenderer extends DefaultTableCellRenderer implements TableCellRenderer {
	DateTimeFormatter customDateFormatter;

	public LocalDateCellRenderer(DateTimeFormatter customDateFormatter) {
		this.customDateFormatter = customDateFormatter;
		setHorizontalAlignment( SwingConstants.CENTER );
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		if (value instanceof LocalDate) {
			LocalDate date = (LocalDate) value;
			
			value = date.format(customDateFormatter);
		}
		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}
}

class MyDateWoYearComparator implements Comparator<LocalDate> {
	public int compare(LocalDate ld1, LocalDate ld2) {
				if (ld1.withYear(0).isBefore(ld2.withYear(0))) return -1;
				if (ld1.withYear(0).isAfter(ld2.withYear(0))) return 1;
				
				if (ld1.withYear(0).equals(ld2.withYear(0))) {
					if (ld1.isBefore(ld2)) return -1;
					if (ld1.isAfter(ld2)) return 1;
				}
				return 0;
			}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MyDateWoYearComparator)
			return true;
		return false;
	}
}