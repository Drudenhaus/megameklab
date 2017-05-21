/*
 * MegaMekLab - Copyright (C) 2017 The MegaMek Team
 *
 * Original author - jtighe (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megameklab.com.ui.Infantry.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.common.AmmoType;
import megamek.common.EquipmentType;
import megamek.common.WeaponType;
import megamek.common.weapons.ArtilleryWeapon;
import megamek.common.weapons.HVACWeapon;
import megamek.common.weapons.LBXACWeapon;
import megamek.common.weapons.RACWeapon;
import megamek.common.weapons.RifleWeapon;
import megamek.common.weapons.UACWeapon;
import megamek.common.weapons.artillery.ArtilleryCannonWeapon;
import megamek.common.weapons.autocannons.ACWeapon;
import megamek.common.weapons.gaussrifles.GaussWeapon;
import megameklab.com.ui.EntitySource;
import megameklab.com.util.EquipmentTableModel;
import megameklab.com.util.IView;
import megameklab.com.util.RefreshListener;
import megameklab.com.util.UnitUtil;
import megameklab.com.util.XTableColumnModel;

/**
 * Shows options for infantry field guns/field artillery
 * 
 * @author Neoancient
 *
 */
public class FieldGunView extends IView implements ActionListener {

    private static final long serialVersionUID = 1130259386466317590L;

    private final static int T_ALL       = 0;
    private final static int T_GUN       = 1;
    private final static int T_ARTILLERY = 2;
    private final static int T_ARTILLERY_CANNON = 3;
    private final static int T_NUM       = 4;

    private RefreshListener refresh;

    private JButton btnSetGun = new JButton("Set Field Gun");
    private JButton btnRemoveGun = new JButton("Remove Field Gun");

    private JComboBox<String> choiceType = new JComboBox<String>();
    private JTextField txtFilter = new JTextField();

    private JRadioButton rbtnStats = new JRadioButton("Stats");
    private JRadioButton rbtnFluff = new JRadioButton("Fluff");

    private TableRowSorter<EquipmentTableModel> equipmentSorter;

    private EquipmentTableModel masterEquipmentList;
    private JTable masterEquipmentTable = new JTable();
    private JScrollPane masterEquipmentScroll = new JScrollPane();

    public static String getTypeName(int type) {
        switch(type) {
        case T_ALL:
            return "All Weapons";
        case T_GUN:
            return "Field Gun";
        case T_ARTILLERY:
            return "Artillery";
        case T_ARTILLERY_CANNON:
            return "Artillery Cannon";
        default:
            return "?";
        }
    }

    public FieldGunView(EntitySource eSource) {
        super(eSource);

        masterEquipmentList = new EquipmentTableModel(eSource.getEntity());
        masterEquipmentTable.setModel(masterEquipmentList);
        masterEquipmentTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        equipmentSorter = new TableRowSorter<EquipmentTableModel>(masterEquipmentList);
        equipmentSorter.setComparator(EquipmentTableModel.COL_DAMAGE, new WeaponDamageSorter());
        equipmentSorter.setComparator(EquipmentTableModel.COL_COST, new FormattedNumberSorter());
        masterEquipmentTable.setRowSorter(equipmentSorter);
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(EquipmentTableModel.COL_NAME, SortOrder.ASCENDING));
        equipmentSorter.setSortKeys(sortKeys);
        XTableColumnModel equipColumnModel = new XTableColumnModel();
        masterEquipmentTable.setColumnModel(equipColumnModel);
        masterEquipmentTable.createDefaultColumnsFromModel();
        TableColumn column = null;
        for (int i = 0; i < EquipmentTableModel.N_COL; i++) {
            column = masterEquipmentTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(masterEquipmentList.getColumnWidth(i));
            column.setCellRenderer(masterEquipmentList.getRenderer());
        }
        masterEquipmentTable.setIntercellSpacing(new Dimension(0, 0));
        masterEquipmentTable.setShowGrid(false);
        masterEquipmentTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        masterEquipmentTable.setDoubleBuffered(true);
        masterEquipmentScroll.setViewportView(masterEquipmentTable);
        masterEquipmentTable.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                int view = masterEquipmentTable.getSelectedRow();
                btnSetGun.setEnabled(view >= 0);
            }
        });
        masterEquipmentScroll.setMinimumSize(new Dimension(200,200));
        masterEquipmentScroll.setPreferredSize(new Dimension(200,200));

        Enumeration<EquipmentType> miscTypes = EquipmentType.getAllTypes();
        ArrayList<EquipmentType> allTypes = new ArrayList<EquipmentType>();
        while (miscTypes.hasMoreElements()) {
            EquipmentType eq = miscTypes.nextElement();
            if(!(eq instanceof WeaponType)
                    || ((WeaponType)eq).isCapital()) {
                continue;
            }
            if ((eq instanceof ACWeapon && !(eq instanceof HVACWeapon))
                    || eq instanceof RACWeapon
                    || eq instanceof UACWeapon
                    || eq instanceof RifleWeapon
                    || eq instanceof ArtilleryCannonWeapon) {
                allTypes.add(eq);
            }
            if ((eq instanceof LBXACWeapon)) {
                allTypes.add(eq);
            }
            if (eq instanceof GaussWeapon
                    && ((WeaponType)eq).getAmmoType() != AmmoType.T_GAUSS_HEAVY
                    && ((WeaponType)eq).getAmmoType() != AmmoType.T_IGAUSS_HEAVY
                    && ((WeaponType)eq).getAmmoType() != AmmoType.T_MAGSHOT                    
                    && ((WeaponType)eq).getAmmoType() != AmmoType.T_HAG) {
                allTypes.add(eq);
            }
            if (eq instanceof ArtilleryWeapon
                    && !eq.hasFlag(WeaponType.F_BA_WEAPON)
                    && ((WeaponType)eq).getAmmoType() != AmmoType.T_CRUISE_MISSILE) {
                allTypes.add(eq);
            }
        }

        masterEquipmentList.setData(allTypes);

        DefaultComboBoxModel<String> typeModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < T_NUM; i++) {
            typeModel.addElement(getTypeName(i));
        }
        choiceType.setModel(typeModel);
        choiceType.setSelectedIndex(1);
        choiceType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterEquipment();
            }
        });

        txtFilter.setText("");
        txtFilter.setMinimumSize(new java.awt.Dimension(200, 28));
        txtFilter.setPreferredSize(new java.awt.Dimension(200, 28));
        txtFilter.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                filterEquipment();
            }
            public void insertUpdate(DocumentEvent e) {
                filterEquipment();
            }
            public void removeUpdate(DocumentEvent e) {
                filterEquipment();
            }
        });

        ButtonGroup bgroupView = new ButtonGroup();
        bgroupView.add(rbtnStats);
        bgroupView.add(rbtnFluff);

        rbtnStats.setSelected(true);
        rbtnStats.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setEquipmentView();
            }
        });
        rbtnFluff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setEquipmentView();
            }
        });
        JPanel viewPanel = new JPanel(new GridLayout(0,2));
        viewPanel.add(rbtnStats);
        viewPanel.add(rbtnFluff);
        setEquipmentView();

        JPanel btnPanel = new JPanel(new GridLayout(0,2));
        btnPanel.add(btnSetGun);
        btnPanel.add(btnRemoveGun);

        //layout
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();

        JPanel databasePanel = new JPanel(new GridBagLayout());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        databasePanel.add(btnPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        databasePanel.add(choiceType, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        databasePanel.add(txtFilter, gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        databasePanel.add(viewPanel, gbc);

        gbc.insets = new Insets(2,0,0,0);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.fill = java.awt.GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        databasePanel.add(masterEquipmentScroll, gbc);

        setLayout(new BorderLayout());
        this.add(databasePanel, BorderLayout.CENTER);
    }

    public void addRefreshedListener(RefreshListener l) {
        refresh = l;
    }

    public void refresh() {
        removeAllListeners();
        filterEquipment();
        btnRemoveGun.setEnabled(getInfantry().hasFieldGun());
        addAllListeners();
    }

    private void removeAllListeners() {
        btnSetGun.removeActionListener(this);
        btnRemoveGun.removeActionListener(this);
    }

    private void addAllListeners() {
        btnSetGun.addActionListener(this);
        btnRemoveGun.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource().equals(btnSetGun)) {
            int view = masterEquipmentTable.getSelectedRow();
            if(view < 0) {
                //selection got filtered away
                return;
            }
            int selected = masterEquipmentTable.convertRowIndexToModel(view);
            EquipmentType equip = masterEquipmentList.getType(selected);
            int num;
            if (equip instanceof ArtilleryWeapon
                    || equip instanceof ArtilleryCannonWeapon) {
                num = 1;
            } else {
                int crewReq = Math.max(2, (int)Math.ceil(equip.getTonnage(getInfantry())));
                num = getInfantry().getShootingStrength() / crewReq;
            }
            UnitUtil.replaceFieldGun(getInfantry(), (WeaponType)equip, num);
        } else if (e.getSource().equals(btnRemoveGun)) {
            UnitUtil.replaceFieldGun(getInfantry(), null, 0);
        } else {
            return;
        }
        refresh.refreshAll();
    }

    private void filterEquipment() {
        RowFilter<EquipmentTableModel, Integer> equipmentTypeFilter = null;
        final int nType = choiceType.getSelectedIndex();
        equipmentTypeFilter = new RowFilter<EquipmentTableModel,Integer>() {
            @Override
            public boolean include(Entry<? extends EquipmentTableModel, ? extends Integer> entry) {
                EquipmentTableModel equipModel = entry.getModel();
                EquipmentType etype = equipModel.getType(entry.getIdentifier());
                if(!UnitUtil.isLegal(getInfantry(), etype)) {
                    return false;
                }
                if ((nType == T_ALL)
                        || ((nType == T_GUN) 
                                && !(etype instanceof ArtilleryWeapon)
                                && !(etype instanceof ArtilleryCannonWeapon))
                        || ((nType == T_ARTILLERY) && etype instanceof ArtilleryWeapon)
                        || ((nType == T_ARTILLERY_CANNON) && etype instanceof ArtilleryCannonWeapon)
                        ) {
                    if(txtFilter.getText().length() > 0) {
                        String text = txtFilter.getText();
                        return etype.getName().toLowerCase().contains(text.toLowerCase());
                    } else {
                        return true;
                    }
                }
                return false;
            }
        };
        equipmentSorter.setRowFilter(equipmentTypeFilter);
    }

    public void setEquipmentView() {
        XTableColumnModel columnModel = (XTableColumnModel)masterEquipmentTable.getColumnModel();
        if(rbtnStats.isSelected()) {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DAMAGE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DIVISOR), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_SPECIAL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_HEAT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_MRANGE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_RANGE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_SHOTS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_TECH), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_TRATING), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DPROTOTYPE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DPRODUCTION), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DCOMMON), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DEXTINCT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DREINTRO), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_COST), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_CREW), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_BV), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_TON), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_CRIT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_REF), true);
        } else {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DAMAGE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DIVISOR), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_SPECIAL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_HEAT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_MRANGE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_RANGE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_SHOTS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_TECH), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_TRATING), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DPROTOTYPE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DPRODUCTION), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DCOMMON), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DEXTINCT), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_DREINTRO), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_COST), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_CREW), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_BV), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_TON), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_CRIT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(EquipmentTableModel.COL_REF), true);
        }
    }



    /**
     * A comparator for integers written as strings with "-" sorted to the bottom always
     * @author Jay Lawson
     *
     */
    public class WeaponIntegerSorter implements Comparator<String> {

        @Override
        public int compare(String s0, String s1) {
            if(s0.equals("-") && s1.equals("-")) {
                return 0;
            } else if(s0.equals("-")) {
                return 1;
            } else if(s1.equals("-")) {
                return -1;
            } else {
                //get the numbers associated with each string
                int r0 = Integer.parseInt(s0);
                int r1 = Integer.parseInt(s1);
                return ((Comparable<Integer>)r1).compareTo(r0);
            }
        }
    }

    public class WeaponDamageSorter implements Comparator<String> {

        @Override
        public int compare(String s0, String s1) {
            //get the numbers associated with each string
            double r1 = parseDamage(s1);
            double r0 = parseDamage(s0);
            return ((Comparable<Double>)r1).compareTo(r0);
        }

        private double parseDamage(String s) {
            double damage = 0;
            damage = Double.parseDouble(s);
            return damage;
        }
    }

    /**
     * A comparator for numbers that have been formatted with DecimalFormat
     * @author Jay Lawson
     *
     */
    public static class FormattedNumberSorter implements Comparator<String> {

        @Override
        public int compare(String s0, String s1) {
            //lets find the weight class integer for each name
            DecimalFormat format = new DecimalFormat();
            int l0 = 0;
            try {
                l0 = format.parse(s0).intValue();
            } catch (java.text.ParseException e) {
                e.printStackTrace();
            }
            int l1 = 0;
            try {
                l1 = format.parse(s1).intValue();
            } catch (java.text.ParseException e) {
                e.printStackTrace();
            }
            return ((Comparable<Integer>)l0).compareTo(l1);
        }
    }
}
