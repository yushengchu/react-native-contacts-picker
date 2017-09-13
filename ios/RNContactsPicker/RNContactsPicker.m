//
//  RNContactsPicker.m
//  RNContactsPicker
//
//  Created by joker on 2017/9/12.
//  Copyright © 2017年 joker. All rights reserved.
//

#import "RNContactsPicker.h"
#import "RCTBridgeModule.h"
#import "RCTBundleURLProvider.h"

//通讯录
#import <AddressBookUI/AddressBookUI.h>
#import <AddressBook/AddressBook.h>


@interface RNContactsPicker()<RCTBridgeModule,ABPeoplePickerNavigationControllerDelegate>

@property (copy,nonatomic) RCTResponseSenderBlock responseCallBack;
@property (nonatomic, strong) ABPeoplePickerNavigationController * peoplePicker;

@end

@implementation RNContactsPicker

RCT_EXPORT_MODULE(RNContactPicker)

//打开通讯录选择器
RCT_EXPORT_METHOD(openContactPicker:(RCTResponseSenderBlock)callback){
    _responseCallBack = callback;
    dispatch_async(dispatch_get_main_queue(), ^{
        if (_peoplePicker == nil) {
            _peoplePicker = [[ABPeoplePickerNavigationController alloc] init];
            _peoplePicker.displayedProperties = @[@(kABPersonPhoneProperty)];
            _peoplePicker.peoplePickerDelegate = self;
        }
        
        [[self topViewController] presentViewController:_peoplePicker animated:YES completion:NULL];
    });
}

//获取全部通讯录
RCT_EXPORT_METHOD(getAllContact:(RCTResponseSenderBlock)callback){
    _responseCallBack = callback;
    dispatch_async(dispatch_get_main_queue(), ^{
        ABAddressBookRef addressBookRef = ABAddressBookCreate();
        CFArrayRef arrayRef = ABAddressBookCopyArrayOfAllPeople(addressBookRef);
        long count = CFArrayGetCount(arrayRef);
        
        NSMutableArray* allContact = [[NSMutableArray alloc]initWithCapacity:0];
        
        for (int i = 0; i < count; i++) {
            //获取联系人对象的引用
            ABRecordRef people = CFArrayGetValueAtIndex(arrayRef, i);
            //获取当前联系人名字
            NSString *firstName=(__bridge NSString *)(ABRecordCopyValue(people, kABPersonFirstNameProperty));
            
            //获取当前联系人姓氏
            NSString *lastName=(__bridge NSString *)(ABRecordCopyValue(people, kABPersonLastNameProperty));
            NSLog(@"--------------------------------------------------");
            
            if (!firstName) {
                firstName = @"";
            }
            if (!lastName) {
                lastName = @"";
            }
            
            NSString *nameStr = [NSString stringWithFormat:@"%@%@",lastName,firstName];
            NSLog(@"userName=%@",nameStr);
            
            //获取当前联系人的电话 数组
            NSMutableArray *phoneArray = [[NSMutableArray alloc]init];
            ABMultiValueRef phones = ABRecordCopyValue(people, kABPersonPhoneProperty);
            for (NSInteger j=0; j<ABMultiValueGetCount(phones); j++) {
                NSString *phone = (__bridge NSString *)(ABMultiValueCopyValueAtIndex(phones, j));
                NSLog(@"phone=%@", phone);
                [phoneArray addObject:phone];
            }
            
            NSDictionary* tmp = @{@"name":nameStr,@"phoneArray":phoneArray};
            [allContact addObject:tmp];
        }
        _responseCallBack(@[@{@"code":[NSNumber numberWithInt:0],@"data":allContact}]);
    });
    
}

//判断是否有通讯录权限
RCT_EXPORT_METHOD(checkContactPermissions:(RCTResponseSenderBlock)callback){
    _responseCallBack = callback;
    dispatch_async(dispatch_get_main_queue(), ^{
        CFErrorRef *error = nil;
        ABAddressBookRef addressBook = ABAddressBookCreateWithOptions(nil, error);
        ABAddressBookRequestAccessWithCompletion(addressBook, ^(bool granted, CFErrorRef error) {
            NSLog(@"granted==%d",granted);
            if (granted) {
                NSDictionary* result  = @{@"status":[NSNumber numberWithBool:true]};
                _responseCallBack(@[result]);
            } else {
                NSDictionary* result  = @{@"status":[NSNumber numberWithBool:false]};
                _responseCallBack(@[result]);
            }
        });
    });
}

#pragma mark - ABPeoplePickerNavigationControllerDelegate
- (void)peoplePickerNavigationControllerDidCancel:(ABPeoplePickerNavigationController *)picker{
    _responseCallBack(@[@{@"code":[NSNumber numberWithInt:2],@"msg":@"用户主动取消"}]);
    [_peoplePicker dismissViewControllerAnimated:YES completion:NULL];
}

// use in iOS7 or earlier
- (BOOL)peoplePickerNavigationController:(ABPeoplePickerNavigationController *)peoplePicker shouldContinueAfterSelectingPerson:(ABRecordRef)person{
    return YES;
}

// use in iOS7 or earlier
- (BOOL)peoplePickerNavigationController:(ABPeoplePickerNavigationController *)peoplePicker shouldContinueAfterSelectingPerson:(ABRecordRef)person property:(ABPropertyID)property identifier:(ABMultiValueIdentifier)identifier{
    [self handleSelectPerson:person property:property identifier:identifier];
    _peoplePicker = nil;
    return NO;
}

// use in iOS8
- (void)peoplePickerNavigationController:(ABPeoplePickerNavigationController *)peoplePicker didSelectPerson:(ABRecordRef)person property:(ABPropertyID)property identifier:(ABMultiValueIdentifier)identifier{
    [self handleSelectPerson:person property:property identifier:identifier];
}

// handle
- (void)handleSelectPerson:(ABRecordRef)person property:(ABPropertyID)property identifier:(ABMultiValueIdentifier)identifier{
    [_peoplePicker dismissViewControllerAnimated:YES completion:NULL];
    
    ABMultiValueRef phoneNumbers = ABRecordCopyValue(person, property);
    NSString *phone = nil;
    if ((ABMultiValueGetCount(phoneNumbers) > 0)) {
        phone = (NSString *)CFBridgingRelease(ABMultiValueCopyValueAtIndex(phoneNumbers, identifier));
    }
    if (phone == nil) {
        phone = @"";
    }
    NSString *phoneStr = [NSString stringWithString:phone];
    
    NSString *firstName = (NSString *)CFBridgingRelease(ABRecordCopyValue(person, kABPersonFirstNameProperty));
    NSString *lastName = (NSString *)CFBridgingRelease(ABRecordCopyValue(person, kABPersonLastNameProperty));
    if (!firstName) {
        firstName = @"";
    }
    if (!lastName) {
        lastName = @"";
    }
    NSString *nameStr = [NSString stringWithFormat:@"%@ %@",lastName,firstName];
    
    NSDictionary* result = @{@"code":[NSNumber numberWithInt:0],@"data":@{@"name":nameStr,@"phone":phoneStr}};
    
    _responseCallBack(@[result]);
    
    _peoplePicker = nil;
    
    NSLog(@"nameStr ---> %@,phoneStr ---> %@",nameStr,phoneStr);
}


//获取当前视图控制器
- (UIViewController*)topViewController {
    return [self topViewControllerWithRootViewController:[UIApplication sharedApplication].keyWindow.rootViewController];
}

- (UIViewController*)topViewControllerWithRootViewController:(UIViewController*)rootViewController {
    if ([rootViewController isKindOfClass:[UITabBarController class]]) {
        UITabBarController* tabBarController = (UITabBarController*)rootViewController;
        return [self topViewControllerWithRootViewController:tabBarController.selectedViewController];
    } else if ([rootViewController isKindOfClass:[UINavigationController class]]) {
        UINavigationController* navigationController = (UINavigationController*)rootViewController;
        return [self topViewControllerWithRootViewController:navigationController.visibleViewController];
    } else if (rootViewController.presentedViewController) {
        UIViewController* presentedViewController = rootViewController.presentedViewController;
        return [self topViewControllerWithRootViewController:presentedViewController];
    } else {
        return rootViewController;
    }
}


@end
