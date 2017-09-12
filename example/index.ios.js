/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */

import React, {Component} from 'react';
import {
    AppRegistry,
    StyleSheet,
    Text,
    View,
} from 'react-native';
import ContactPickerBridge from 'react-native-contacts-picker';

export default class ContactPicker extends Component {
    render() {
        return (
            <View style={styles.container}>
                <Text style={styles.welcome} onPress={this.openContactPicker}>打开通讯录选择器</Text>
                <Text style={styles.welcome} onPress={this.getAllContact}>获取全部通讯录</Text>
                <Text style={styles.welcome} onPress={this.checkContactPermissions}>是否有通讯录权限</Text>
            </View>
        );
    }

    openContactPicker = () => {
        ContactPickerBridge.openContactPicker((result) => {
            console.log('openContactPicker ---->', JSON.stringify(result));
        });
    };

    getAllContact = () => {
        ContactPickerBridge
            .getAllContact((result) => {
                console.log('getAllContact ---->', JSON.stringify(result));
            });
    };

    checkContactPermissions = () => {
        ContactPickerBridge
            .checkContactPermissions((result) => {
                console.log('getAllContact ---->', JSON.stringify(result));
            });
    };
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#F5FCFF',
    },
    welcome: {
        fontSize: 20,
        textAlign: 'center',
        margin: 10,
    },
    instructions: {
        textAlign: 'center',
        color: '#333333',
        marginBottom: 5,
    },
});

AppRegistry.registerComponent('ContactPicker', () => ContactPicker);
