package com.viktorx.skyblockbot.task.buyItem;

public enum BuyItemState {
    IDLE,
    LOADING_AUCTIONS,
    SENDING_COMMAND,
    WAITING_FOR_MENU,
    BUYING,
    CONFIRMING_BUY,
    CHECKING_BUY_RESULT,
    RESTARTING, // if item wasn't bought because some hypixel error, like someone else already bought it we restart
    CLAIMING_AUCTION, // if item was bought, but didn't go to our inventory, we have to claim it
    CLAIMING_AUCTION_VIEW_BIDS,
    CLAIMING_AUCTION_BID,
    CLIAMING_AUCTION_CLAIM,
    PAUSED;


    BuyItemState() {

    }
}
