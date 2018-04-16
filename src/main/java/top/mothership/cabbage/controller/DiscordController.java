package top.mothership.cabbage.controller;

import net.dv8tion.jda.client.entities.Group;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.consts.OverallConsts;

import javax.annotation.PostConstruct;
import javax.security.auth.login.LoginException;
import java.util.List;
import java.util.Random;

@Component
public class DiscordController extends ListenerAdapter {
    @PostConstruct
    public void handleDiscordMessage() {
        try {
            JDA jda = new JDABuilder(AccountType.BOT)
                    .setToken(OverallConsts.CABBAGE_CONFIG.getString("discordToken"))
                    //The token of the account that is logging in.
                    //An instance of a class that will handle events.
                    .addEventListener(new DiscordController())
                    .buildBlocking();
            //There are 2 ways to login, blocking vs async. 阻塞保证JDA被完整加载
        } catch (LoginException e) {
            //认证错误会抛这个异常
            e.printStackTrace();
        } catch (InterruptedException e) {
            //因为buildBlocking是一个阻塞方法，在JDA完整加载前会一直等待
            // 如果等待被打断，会抛出这个异常，在这个特别简单的例子里这个异常永远不会发生，事实上它只会发生在buildBlocking方法在一个可能被打断的线程里运行时。
            e.printStackTrace();
        }
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        JDA jda = event.getJDA();
        //JDA, the core of the api.
        long responseNumber = event.getResponseNumber();
        //The amount of discord events that JDA has received since the last reconnect.

        //Event specific information
        User author = event.getAuthor();
        //The user that sent the message
        Message message = event.getMessage();
        //The message that was received.
        MessageChannel channel = event.getChannel();
        //This is the MessageChannel that the message was sent to.
        //  This could be a TextChannel, PrivateChannel, or Group!

        String msg = message.getContentDisplay();
        //This returns a human readable version of the Message. Similar to
        // what you would see in the client.
        //一条人类可读的消息，与客户端里看到的类似

        boolean bot = author.isBot();
        //This boolean is useful to determine if the User that
        // sent the Message is a BOT or not!
        //是不是Bot

        if (event.isFromType(ChannelType.TEXT)) {
            //If this message was sent to a Guild TextChannel
            //如果这条消息是在公会的文字频道里发送的
            //Because we now know that this message was sent in a Guild, we can do guild specific things
            // Note, if you don't check the ChannelType before using these methods, they might return null due
            // the message possibly not being from a Guild!

            //先鉴别消息来源，如果来自公会才做一些特别的事情，否则可能会触发NPE
            Guild guild = event.getGuild();
            //The Guild that this message was sent in. (note, in the API, Guilds are Servers)
            //在API中公会是服务器的意思
            TextChannel textChannel = event.getTextChannel();
            //The TextChannel that this message was sent to.
            Member member = event.getMember();
            //member包含了一些某个玩家在公会中的特殊信息
            //This Member that sent the message. Contains Guild specific information about the User!

            String name;
            if (message.isWebhookMessage()) {
                name = author.getName();
                //If this is a Webhook message, then there is no Member associated
            }
            // with the User, thus we default to the author for name.
            else {
                //类似群名片的机制？
                name = member.getEffectiveName();
                //This will either use the Member's nickname if they have one,
            }
            // otherwise it will default to their username. (User#getName())

            System.out.printf("(%s)[%s]<%s>: %s\n", guild.getName(), textChannel.getName(), name, msg);
        } else if (event.isFromType(ChannelType.PRIVATE)) {
            //If this message was sent to a PrivateChannel

            //The message was sent in a PrivateChannel.
            //In this example we don't directly use the privateChannel, however, be sure, there are uses for it!
            //在例子中不直接使用私聊频道
            PrivateChannel privateChannel = event.getPrivateChannel();

            System.out.printf("[PRIV]<%s>: %s\n", author.getName(), msg);
        } else if (event.isFromType(ChannelType.GROUP)) {
            //If this message was sent to a Group. This is CLIENT only!

            //The message was sent in a Group. It should be noted that Groups are CLIENT only.
            Group group = event.getGroup();
            String groupName = group.getName() != null ? group.getName() : "";
            //A group name can be null due to it being unnamed.

            System.out.printf("[GRP: %s]<%s>: %s\n", groupName, author.getName(), msg);
        }


        //Now that you have a grasp on the things that you might see in an event, specifically MessageReceivedEvent,
        // we will look at sending / responding to messages!
        //这里只做消息回复，是一个最简单的例子
        //This will be an extremely simplified example of command processing.

        //Remember, in all of these .equals checks it is actually comparing
        // message.getContentDisplay().equals, which is comparing a string to a string.
        // If you did message.equals() it will fail because you would be comparing a Message to a String!

        //实际上是在比较message.getContentDisplay().equals，这样才是字符串比较，而不是msg比较（那是个pojo类）
        if (msg.equals("!ping")) {
            //This will send a message, "pong!", by constructing a RestAction and "queueing" the action with the Requester.
            // By calling queue(), we send the Request to the Requester which will send it to discord. Using queue() or any
            // of its different forms will handle ratelimiting for you automatically!
            //构造一条pong！消息。通过调用queue方法，我们给发送器发送一条请求，使用queue或者queue的不同形式会自动处理速度上限
            channel.sendMessage("pong!").queue();
        } else if (msg.equals("!roll")) {
            //In this case, we have an example showing how to use the Success consumer for a RestAction. The Success consumer
            // will provide you with the object that results after you execute your RestAction. As a note, not all RestActions
            // have object returns and will instead have Void returns. You can still use the success consumer to determine when
            // the action has been completed!
            //这是一个在RestAction使用成功消费者的例子。
            // 成功消费者会在你执行RestAction后给你一个对象作为结果，
            // 并不是所有的RestAction都会给一个对象，有的会返回void。
            // 可以用成功消费者来确认什么时候这个动作被完成

            Random rand = new Random();
            int roll = rand.nextInt(6) + 1;
            //This results in 1 - 6 (instead of 0 - 5)
            channel.sendMessage("Your roll: " + roll).queue(sentMessage -> {
                //用lambda表达式
                if (roll < 3) {
                    channel.sendMessage("The roll for messageId: " + sentMessage.getId() + " wasn't very good... Must be bad luck!\n").queue();
                }
            });
        } else if (msg.startsWith("!kick"))
        //Note, I used "startsWith, not equals.
        {
            //This is an admin command. That means that it requires specific permissions to use it, in this case
            // it needs Permission.KICK_MEMBERS. We will have a check before we attempt to kick members to see
            // if the logged in account actually has the permission, but considering something could change after our
            // check we should also take into account the possibility that we don't have permission anymore, thus Discord
            // response with a permission failure!
            //We will use the error consumer, the second parameter in queue!
            //管理员命令：需要特殊权限，在这个例子里需要提出成员的权限。
            //
            //We only want to deal with message sent in a Guild.
            if (message.isFromType(ChannelType.TEXT)) {
                //If no users are provided, we can't kick anyone!
                if (message.getMentionedUsers().isEmpty()) {
                    channel.sendMessage("You must mention 1 or more Users to be kicked!").queue();
                } else {
                    Guild guild = event.getGuild();
                    Member selfMember = guild.getSelfMember();
                    //This is the currently logged in account's Member object.
                    // Very similar to JDA#getSelfUser()!

                    //Now, we the the logged in account doesn't have permission to kick members.. well.. we can't kick!
                    if (!selfMember.hasPermission(Permission.KICK_MEMBERS)) {
                        channel.sendMessage("Sorry! I don't have permission to kick members in this Guild!").queue();
                        return; //We jump out of the method instead of using cascading if/else
                    }

                    //Loop over all mentioned users, kicking them one at a time. Mwauahahah!
                    List<User> mentionedUsers = message.getMentionedUsers();
                    for (User user : mentionedUsers) {
                        Member member = guild.getMember(user);
                        //We get the member object for each mentioned user to kick them!

                        //We need to make sure that we can interact with them. Interacting with a Member means you are higher
                        // in the Role hierarchy than they are. Remember, NO ONE is above the Guild's Owner. (Guild#getOwner())
                        if (!selfMember.canInteract(member)) {
                            // use the MessageAction to construct the content in StringBuilder syntax using append calls
                            channel.sendMessage("Cannot kick member: ")
                                    .append(member.getEffectiveName())
                                    .append(", they are higher in the hierarchy than I am!")
                                    .queue();
                            continue;   //Continue to the next mentioned user to be kicked.
                        }

                        //Remember, due to the fact that we're using queue we will never have to deal with RateLimits.
                        // JDA will do it all for you so long as you are using queue!
                        guild.getController().kick(member).queue(
                                success -> channel.sendMessage("Kicked ").append(member.getEffectiveName()).append("! Cya!").queue(),
                                error ->
                                {
                                    //The failure consumer provides a throwable. In this case we want to check for a PermissionException.
                                    if (error instanceof PermissionException) {
                                        PermissionException pe = (PermissionException) error;
                                        Permission missingPermission = pe.getPermission();
                                        //If you want to know exactly what permission is missing, this is how.
                                        //Note: some PermissionExceptions have no permission provided, only an error message!

                                        channel.sendMessage("PermissionError kicking [")
                                                .append(member.getEffectiveName()).append("]: ")
                                                .append(error.getMessage()).queue();
                                    } else {
                                        channel.sendMessage("Unknown error while kicking [")
                                                .append(member.getEffectiveName())
                                                .append("]: <").append(error.getClass().getSimpleName()).append(">: ")
                                                .append(error.getMessage()).queue();
                                    }
                                });
                    }
                }
            } else {
                channel.sendMessage("This is a Guild-Only command!").queue();
            }
        } else if (msg.equals("!block")) {
            //This is an example of how to use the complete() method on RestAction.
            //这是一个如何使用complete()方法的例子。
            // The complete method acts similarly to how JDABuilder's buildBlocking works,
            // it waits until the request has been sent before continuing execution.
            // 这个方法和JDABuilder的buildBlocking方法类似，在请求被发送前会等待。
            //Most developers probably wont need this and can just use queue.
            //大部分开发者用不到这个，只用队列就行了。
            // If you use complete, JDA will still handle ratelimit  control,
            //如果用了，JDA也会处理速率限制问题
            // however if should Queue is false
            // it won't queue the Request to be sent after the ratelimit retry after time is past.
            // It will instead fire a RateLimitException!
            //然而，如果队列失败了，它不会在速度限制重试等待时间之后继续发送队列该发送的请求
            // One of the major advantages of complete() is that
            // it returns the object that queue's success consumer would have,
            //complete()方法的最大优点是，它返回一个队列成功后的消费者会拥有的对象
            // but it does it in the same execution context as when the request was made.
            // This may be important for most developers,
            // but, honestly, queue is most likely what developers will want to use as it is faster.
            //但是他会在和请求被构造时的相同的执行上下文中做这件事，这可能对大多数开发者很重要，但是诚实的说，开发者会更想使用队列，毕竟这个比较快
            try {
                //Note the fact that complete returns the Message object!
                //注意，complete()方法会返回消息对象
                //The complete() overload queues the Message for execution and will return when the message was sent
                //complete()方法重载了要执行的消息的队列，并且在消息被发送时返回
                //It does handle rate limits automatically
                Message sentMessage = channel.sendMessage("I blocked and will return the message!").complete();
                //This should only be used if you are expecting to handle rate limits yourself
                //你只应该在自己处理消息速度的时候用它
                //如果消息速度达到上限，这次发送会失败并且抛出异常
                //The completion will not succeed if a rate limit is breached and throw a RateLimitException
                Message sentRatelimitMessage = channel.sendMessage("I expect rate limitation and know how to handle it!").complete(false);

                System.out.println("Sent a message using blocking! Luckly I didn't get Ratelimited... MessageId: " + sentMessage.getId());
            } catch (RateLimitedException e) {
                System.out.println("Whoops! Got ratelimited when attempting to use a .complete() on a RestAction! RetryAfter: " + e.getRetryAfter());
            }
            //Note that RateLimitException is the only checked-exception thrown by .complete()
            catch (RuntimeException e) {
                System.out.println("Unfortunately something went wrong when we tried to send the Message and .complete() threw an Exception.");
                e.printStackTrace();
            }
        }
    }
}
