//
//  ViewController.m
//  SocketRocketDemo
//
//  Created by Wang on 2017/5/24.
//  Copyright © 2017年 云客. All rights reserved.
//

#import "ViewController.h"
#import <SocketRocket/SocketRocket.h>

@interface ViewController ()<SRWebSocketDelegate>

@property (weak, nonatomic) IBOutlet UITextField *urlTF;

@property (weak, nonatomic) IBOutlet UITextField *msgSendTF;

@property (weak, nonatomic) IBOutlet UITextView *msgReceiveTW;

@property (nonatomic, strong) SRWebSocket *socket;

@end

#define MAX_ALLOW_MISS_COUNT 5
#define SECONDS_PER_PING 4

static NSString *socketUrl = @"ws://192.168.2.123:7397/websocket";

@implementation ViewController {
    NSTimer *_pingTimer;
    NSTimer *_reconnetTimer;

    NSInteger _missedPongCount;
    BOOL _continueSendPing;

    BOOL _reconnectFlag;
    NSInteger _reconnectCount;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.
    [self prepareReconnect];
}

- (void)startTimer {
    __weak typeof(self) weakself = self;
    _pingTimer = [NSTimer scheduledTimerWithTimeInterval:4 repeats:YES block:^(NSTimer *timer) {
        [weakself sendPingClick:nil];
    }];
}

- (IBAction)registerClick:(id)sender {
    if (self.socket.readyState == SR_OPEN) {
        NSString *sendName = [NSString stringWithFormat:@"{\"type\":\"name\",\"name\":\"%@\"}", _urlTF.text];
        [self.socket send:sendName];
    }
}


- (void)prepareReconnect {
    if (_reconnectFlag && _reconnectCount >= 1) {
        return;
    }
    _continueSendPing = NO;
    _reconnectFlag = YES;
    _reconnectCount = 1;
    [self reconnect];
}

// 重新连接
- (void)reconnect {
    if (_reconnectFlag) {  // 需要重新连接
        self.socket = [[SRWebSocket alloc] initWithURL:[NSURL URLWithString:socketUrl]];
        self.socket.delegate = self;
        NSLog(@"%@", [NSString stringWithFormat:@"第 %ld 次 重新连接...", (long)_reconnectCount]);
        [self.socket open];

        _reconnectCount ++;

        // 设置下一次的触发时间
        NSInteger nextSecond = 1 << MIN(_reconnectCount, 5);
        NSLog(@"%ld秒后继续第 %ld 次重连..", (long)nextSecond, (long)_reconnectCount);
        [self performSelector:@selector(reconnect) withObject:nil afterDelay:nextSecond];
    } else {
        NSLog(@"next connect 废弃");
    }
}

- (IBAction)sendPingClick:(id)sender {
    if (_continueSendPing) {
        _missedPongCount ++;
        if (_missedPongCount >= MAX_ALLOW_MISS_COUNT) {
            [self prepareReconnect];
        } else {
            [self.socket sendPing:[@"Y" dataUsingEncoding:NSUTF8StringEncoding]];
        }
    }
}

- (IBAction)msgSendClick:(id)sender {
    if (_msgSendTF.text.length && self.socket.readyState == SR_OPEN) {
        NSString *msg = [NSString stringWithFormat:@"{\"type\":\"msg\",\"msg\":\"%@\"}", _msgSendTF.text];
        [self.socket send:msg];
    }
}

- (void)receiveNewMessage:(NSString *)msg {
    _msgReceiveTW.text = [NSString stringWithFormat:@"%@\n%@", _msgReceiveTW.text, msg];
    [_msgReceiveTW scrollRangeToVisible:NSMakeRange(_msgReceiveTW.text.length - 1, 1)];
}

// message will either be an NSString if the server is using text
// or NSData if the server is using binary.
- (void)webSocket:(SRWebSocket *)webSocket didReceiveMessage:(id)message {
    if ([message isKindOfClass:[NSString class]]) {
        [self receiveNewMessage:message];
    }
    NSLog(@"receive message : %@", message);
}

- (void)webSocketDidOpen:(SRWebSocket *)webSocket {
    _continueSendPing = YES;
    _missedPongCount = 0;
    _reconnectFlag = NO;
    [self receiveNewMessage:@"local: socket连接成功...请注册"];
    NSLog(@"webSocket did open : %@", webSocket);
    [self startTimer];
}

- (void)webSocket:(SRWebSocket *)webSocket didFailWithError:(NSError *)error {
    NSLog(@"webSocket did fail : %@", error.localizedFailureReason);
    if (webSocket.readyState == SR_CLOSED) {
        [self prepareReconnect];
    }
}

- (void)webSocket:(SRWebSocket *)webSocket didCloseWithCode:(NSInteger)code reason:(NSString *)reason wasClean:(BOOL)wasClean {
    [self receiveNewMessage:@"local: socket关闭..."];
    [self prepareReconnect];
}

- (void)webSocket:(SRWebSocket *)webSocket didReceivePong:(NSData *)pongPayload {
    // 收到以后 将 count 置为0
    _missedPongCount = 0;
    NSLog(@"webSocket did receive pong : %@", [[NSString alloc] initWithData:pongPayload encoding:NSUTF8StringEncoding]);
}

// Return YES to convert messages sent as Text to an NSString. Return NO to skip NSData -> NSString conversion for Text messages. Defaults to YES.
- (BOOL)webSocketShouldConvertTextFrameToString:(SRWebSocket *)webSocket {
    return YES;
}


- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}


@end
