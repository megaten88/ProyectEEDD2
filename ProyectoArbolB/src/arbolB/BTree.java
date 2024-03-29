/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this templgete file, choose Tools | Templgetes
 * and open the templgete in the editor.
 */
package arbolB;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 *
 * @author Agile PC
 */
public class BTree implements Serializable {

    private Node root;
    private int order;
    // Boolean that sets if needs promote
    private boolean promote;
    // our inner index in tree
    private ArrayList<Key> allkeys;
    private int level;

    public BTree(int order) {
        this.order = order;
        this.root = new Node(order);
        this.allkeys = new ArrayList();
        this.promote = false;
        this.level = 0;
    }

    public Node getRoot() {
        return root;
    }

    public int getOrder() {
        return order;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public ArrayList<Key> getAllkeys() {
        return allkeys;
    }

    //The method for Split and  Promote
    public void splitAndPromote(Node node, Key key) {
        if (node.getParent() == null) {
            node.addKey(key);
            int median = node.getMedianPosition();
            Key keySplit = node.getKeys().get(median);
            Node temp = new Node(node.getOrder());
            temp.addKey(keySplit);
            temp.setLevel(temp.getLevel() + 1);
            Node right = node.split();
            right.setLevel(right.getLevel() + 1);
            right.setParent(temp);
            node.setParent(temp);
            temp.getChildren().add(node);
            temp.getChildren().add(right);
            root = temp;
        } else {
            this.promote = true;
            node.addKey(key);
            int median = node.getMedianPosition();
            Key keySplit = node.getKeys().get(median);
            Node right = node.split();
            right.setLevel(right.getLevel() + 1);
            right.setParent(node.getParent());
            ArrayList<Node> tempArray = new ArrayList();
            int index = 0;
            for (int i = 0; i < node.getParent().getChildren().size(); i++) {
                if (node.getParent().getChildren().get(i).equals(node)) {
                    tempArray.add(node.getParent().getChildren().get(i));
                    index = i;
                    break;
                } else {
                    tempArray.add(node.getParent().getChildren().get(i));
                }
            }
            tempArray.add(right);
            for (int i = index; i < node.getParent().getChildren().size(); i++) {
                tempArray.add(node.getParent().getChildren().get(i));
            }
            tempArray.remove(tempArray.lastIndexOf(node));
            node.getParent().setChildren(tempArray);
            insert(node.getParent(), keySplit);
        }
    }

    // It inserts the key in BTree, and in the index (index is only to compare)
    public void insert(Node node, Key key) {
        if (this.allkeys.indexOf(key) == -1) {
            this.allkeys.add(key);
            Collections.sort(allkeys, comparatorKeys);
        }
        if (node.getLevel() == -1) {
            node.setLevel(this.level);
            this.level++;
            node.addKey(key);
            this.allkeys.add(key);
        } else if (!node.getChildren().isEmpty() && !this.promote) {
            int index = 0;
            for (index = 0; index < node.getKeys().size(); index++) {
                if (key.getKey().compareTo(node.getKeys().get(index).getKey()) < 0) {
                    break;
                }
                if (key.getKey().compareTo(node.getKeys().get(index).getKey()) == 0) {
                    return;
                }
            }
            if (index >= node.getChildren().size()) {
                index = node.getChildren().size() - 1;
            }
            insert(node.getChildren().get(index), key);
        } else if (node.getChildren().isEmpty() || this.promote) {
            if (node.getKeys().size() < node.getOrder() - 1) {
                node.addKey(key);
            } else {
                splitAndPromote(node, key);
            }
            this.promote = false;
        }
    }
// Delete calls concat and redistribution if it goes underflow
    public void delete(Node node, Comparable key) {
        int position = 0;
        for (position = 0; position < node.getKeys().size(); position++) {
            if (key.compareTo(node.getKeys().get(position).getKey()) == 0) {
                break;
            }
        }
        if (node.getChildren().isEmpty() || this.promote) {
            node.deleteKey(position);
            if (node.getKeys().size() < ((this.order / 2) - 1) || node.getKeys().isEmpty()) {
                redistribute(node, key);
            }
            this.promote = false;
        } else {
            Node son = node.getChildren().get(0);
            while (!son.getChildren().isEmpty()) {
                son = son.getChildren().get(son.getChildren().size() - 1);
            }
            Key keyTake = node.getKeys().get(position);
            node.getKeys().remove(position);
            node.getKeys().add(position, son.getKeys().get(son.getKeys().size() - 1));
            son.getKeys().remove(son.getKeys().size() - 1);
            son.getKeys().add(keyTake);
            delete(son, key);
        }
        if (root.getChildren().size() > 1) {
            Key rootLast = root.getKeys().get(root.getKeys().size() - 1);
            Key lastSonFirst = root.getChildren().get(root.getChildren().size() - 1).getKeys().get(0);
            if (rootLast.getKey().compareTo(lastSonFirst.getKey()) > 0) {
                Key swap = rootLast;
                rootLast = lastSonFirst;
                lastSonFirst = swap;
                root.getKeys().add(root.getKeys().size() - 1, rootLast);
                root.getKeys().remove(lastSonFirst);
                root.sortKeys();
                root.getChildren().get(root.getChildren().size() - 1).getKeys().add(0, lastSonFirst);
                root.getChildren().get(root.getChildren().size() - 1).getKeys().remove(rootLast);
                root.getChildren().get(root.getChildren().size() - 1).sortKeys();
            }
        }
    }

    private void redistribute(Node node, Comparable key) {
        System.out.println(node);
        if (node.getParent() != null) {
            int position = 0;
            for (position = 0; position < node.getParent().getChildren().size(); position++) {
                if (node.getParent().getChildren().get(position).getLevel() == node.getLevel()) {
                    break;
                }
            }
            boolean take = false;
            if (position - 1 >= 0) {
                Node adyacent = node.getParent().getChildren().get(position - 1);
                if (adyacent.canShare()) {
                    node.addKey(node.getParent().getKeys().get(position - 1));
                    node.getParent().deleteKey(position - 1);
                    Key keyTake = adyacent.getKeys().get(adyacent.getKeys().size() - 1);
                    node.getParent().addKey(keyTake);
                    adyacent.deleteKey(adyacent.getKeys().size() - 1);
                    if (!adyacent.getChildren().isEmpty()) {
                        node.getChildren().add(adyacent.getChildren().get(adyacent.getChildren().size() - 1));
                        adyacent.getChildren().get(adyacent.getChildren().size() - 1).setParent(node);
                        adyacent.deleteChild(adyacent.getChildren().size() - 1);
                        adyacent.sortChildren();
                    }
                    take = true;
                }
            }
            if (position + 1 < node.getParent().getChildren().size() && !take) {
                Node adyacent = node.getParent().getChildren().get(position);
                if (node.getParent().getChildren().get(position).canShare()) {
                    Key keyTake = node.getParent().getKeys().get(position);
                    node.addKey(keyTake);
                    node.getParent().deleteKey(position);
                    Key keyParent = adyacent.getKeys().get(position + 1);
                    node.getParent().addKey(keyParent);
                    adyacent.deleteKey(position + 1);
                    if (!adyacent.getChildren().isEmpty()) {
                        node.getChildren().add(adyacent.getChildren().get(0));
                        adyacent.getChildren().get(0).setParent(node);
                        adyacent.deleteChild(0);
                        adyacent.sortChildren();
                    }

                } else {
                    concat(node, position);
                }
            }
        } else if (node.getKeys().isEmpty()) {
            root = node.getChildren().get(0);
            root.setParent(null);
        }
    }

    private void concat(Node node, int position) {
        if (position - 1 >= 0) {
            Node adyacent = node.getParent().getChildren().get(position - 1);
            for (int i = 0; i < node.getKeys().size(); i++) {
                adyacent.addKey(node.getKeys().get(i));
            }
            for (int i = 0; i < node.getChildren().size(); i++) {
                node.getChildren().get(i).getParent().setParent(adyacent);
                adyacent.getChildren().add(node.getChildren().get(i));
            }
            adyacent.sortChildren();
            node.getParent().deleteChild(position);
            adyacent.addKey(node.getParent().getKeys().get(position - 1));
            this.promote = true;
            delete(node.getParent(), node.getParent().getKeys().get(position - 1).getKey());

        } else if (position + 1 < node.getParent().getChildren().size()) {
            Node adyacent = node.getParent().getChildren().get(position + 1);
            for (int i = 0; i < node.getKeys().size(); i++) {
                adyacent.addKey(node.getKeys().get(i));
            }
            for (int i = 0; i < node.getChildren().size(); i++) {
                node.getChildren().get(i).setParent(adyacent);
                adyacent.getChildren().add(node.getChildren().get(i));
            }
            adyacent.addKey(node.getParent().getKeys().get(position));
            node.getParent().deleteChild(position);
            adyacent.sortChildren();
            node.sortChildren();
            this.promote = true;
            delete(node.getParent(), node.getParent().getKeys().get(position).getKey());
        }
    }
// Returns the node where the key is found
    public Node search(Node node, Comparable key) {
        Node temp = node;
        for (int i = 0; i < node.getKeys().size(); i++) {
            if (key.compareTo(node.getKeys().get(i).getKey()) == 0) {
                temp = node;
                break;
            }
        }
        if (temp.getLevel() != -1) {
            for (int i = 0; i < node.getChildren().size(); i++) {
                if (temp.getPositionKey(key) != -1) {
                    return temp;
                } else {
                    temp = search(node.getChildren().get(i), key);
                    if (temp.getLevel() == -1) {
                        break;
                    }
                }
            }
        }
        return temp;
    }

    //Sorts the index
    public void sortKeys() {
        if (!this.allkeys.isEmpty()) {
            Collections.sort(this.allkeys, comparatorKeys);
        }
    }
    public static Comparator<Key> comparatorKeys = new Comparator<Key>() {
        @Override
        public int compare(Key o1, Key o2) {
            return o1.getKey().compareTo(o2.getKey());
        }

    };
}
