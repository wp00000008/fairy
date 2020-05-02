

#property strict

enum ordertype {
   Instant = 0,
   Pending = 1
};

enum tptype {
   Points = 0,
   Currency = 1
};

enum rztype {
   FixedZone = 0,
   DynamicZone = 1
};

enum addlotstype {
   Ratio = 0,
   Fixed = 1,
   Custom = 2,
   Multiple = 3
};

enum LossSet {
   Disabled = 0,
   PlaceStopLoss = 1,
   CloseTradeByTrade = 2,
   PartialLossRetracement=3
};

input int MagicNumber = 777;
input string Info1 = "=== Entry ====";
input bool EnablePrimaryHeikenAshi = true;
input ENUM_TIMEFRAMES PrimaryHeikenAshiTF = PERIOD_M5;

input bool EnableSecondaryHeikenAshi = false;
input ENUM_TIMEFRAMES SecondaryHeikenAshiTF = PERIOD_M15;

input bool EnableMa = false;
input ENUM_TIMEFRAMES MaTF = PERIOD_M15;
input int MaPeriod = 20;
input int MaShift = 0;
input ENUM_MA_METHOD MaMethod = MODE_SMA;
input ENUM_APPLIED_PRICE MaPrice = PRICE_CLOSE;
input bool EnableMaSlope = false;
input double MaSlopeDeltaPoints = 0;

input int RenkSizePoints = 0;

input bool EnableMACD = false;
input ENUM_TIMEFRAMES MACDTF = PERIOD_M5;
input int MACDFastPeriod = 12;
input int MACDSlowPeriod = 26;
input int MACDSignalPeriod = 9;
input ENUM_APPLIED_PRICE MACDPrice = PRICE_CLOSE;

input string Info2 = "=== Exit for First Order ====";
input int TakeProfitPoints = 150;
input bool EnablePrimaryHeikenAshiExit = false;
input bool EnableSecondaryHeikenAshiExit = false;
input bool EnableRenkoExit = false;

input string Info3 = "=== Recovery Zone ====";
input bool EnableRecoveryZone = true;
input ordertype RecoveryZoneOrderType = Instant;

//input tptype RecoveryZoneTPType = Points;
//input double RecoveryZoneTP = 400;

//input int RecoveryZoneTPInitialPoints = 500;
input int RecoveryZoneTPHedgePoints = 2500;
input double RecoveryZoneTPHedgeCurrency = 0;

input rztype RecoveryZoneType = FixedZone; 
input int FixedRecoveryZonePoints = 250;
input int FixedRecoveryZoneAddPoints = 0;
input int FixedRecoveryZoneAddOrders = 0;
input int FixedRecoveryZoneMaxPoints = 0;

input ENUM_TIMEFRAMES DynamicRecoveryZoneTF = PERIOD_M15;
input int DynamicRecoveryZoneCandles = 14;
input double DynamicRecoveryZoneDivider = 1;
input int DynamicRecoveryZoneMinPoints = 200;
input int DynamicRecoveryZoneMaxPoints = 1000;

input string Info4 = "=== Lots & Orders ====";
input double InitialLots = 0.3;
input addlotstype RecoveryZoneAddLotsType = Multiple;
input double FixedAddLots = 0.01;
input string CustomAddLots = "0.02;0.03;0.05;0.08;0.13;0.21;0.34";
input double MultiplierAddLots = 2.0;
input int MaxOrders = 9;

input string Info5 = "=== Loss ====";
input LossSet LossType = PlaceStopLoss;
input int StopLossPoint = 500;

input string Info6 = "=== Break Even ====";
input int BreakEvenLevel = 2;
input double BreakEvenMinProfit = 3;

input string Info7 = "=== Close Basket ====";
input bool CloseBasketTPPlusBreakEven = false;
input bool CloseBasketAtMaxOrders = false;

bool gb_close_all = false;
string gs_renko = "";
datetime gt_last_renko = 0;

int OnInit()
{
   int cmd;
   
   if (!GetBuySellSignal(cmd)) {
      Alert("All entry down");
      ExpertRemove();
      return -1;
   }
   
   return 0;   
}

void GetLevel(string cmm, int &level, int &hlps)
{
   string arr[];
   
   if (StringSplit(cmm, StringGetCharacter("-",0), arr) != 2) {
      Alert("Error GetLevel: ", cmm);
      ExpertRemove();
   }
   
   level = int(arr[0]);
   hlps = int(arr[1]);   
}

void OnTick()
{
   int ii, bb=0, ss=0, bbp=0, ssp=0, last_bs_tk=-1, closed=0, level, hlps;
   datetime last_bs_tm=0;
   double hh=0, ll=0, ttp=0, last_pt_points=0, lots_buy=0, lots_sell=0, be_profit=0;
   
   if (gb_close_all) {
      if (CloseAll()) {
         gb_close_all = false;
      }
      
      return;
   }   
   
   for (ii=OrdersTotal()-1; ii>=0; ii--) {
      if (!OrderSelect(ii, SELECT_BY_POS)) {
         Print("error orderselect");
         
         return;
      }      
      
      if (OrderSymbol() == Symbol() && OrderMagicNumber() == MagicNumber) {
         if (OrderType() == OP_BUY) {
            bb ++;
            lots_buy += OrderLots();
         }
            
         if (OrderType() == OP_SELL) {
            ss ++;
            lots_sell += OrderLots();
         }  
         
         if (OrderType() == OP_BUY || OrderType() == OP_SELL) { 
            GetLevel(OrderComment(), level, hlps);
            
            ttp += OrderProfit() + OrderSwap() + OrderCommission();
            
            if (BreakEvenLevel >= 0 && (level == 1 || level > BreakEvenLevel)) {
               be_profit += OrderProfit() + OrderSwap() + OrderCommission();
            }  
            
            if (last_bs_tm == 0 || OrderOpenTime() > last_bs_tm) {
               last_bs_tm = OrderOpenTime();
               last_bs_tk = OrderTicket();
               
               if (OrderType() == OP_BUY) {
                  hh = OrderOpenPrice();
                  ll = hh - hlps * Point;
                  
                  last_pt_points = (OrderClosePrice() - OrderOpenPrice()) / Point;
               }
               
               if (OrderType() == OP_SELL) {
                  ll = OrderOpenPrice();
                  hh = ll + hlps * Point;
                  
                  last_pt_points = (OrderOpenPrice() - OrderClosePrice()) / Point;
               }
            }
         }
         
         if (OrderType() == OP_BUYSTOP) bbp ++;
         if (OrderType() == OP_SELLSTOP) ssp ++;
      }
   }
   
   // objects
   
   if (bb + ss == 0) {
      ObjectDelete(0, "hh");
      ObjectDelete(0, "ll");
   } else {
      ObjectCreate(0, "hh", OBJ_HLINE, 0, 0, hh);
      ObjectCreate(0, "ll", OBJ_HLINE, 0, 0, ll);
      
      ObjectSetDouble(0, "hh", OBJPROP_PRICE1, hh);
      ObjectSetDouble(0, "ll", OBJPROP_PRICE1, ll);
   }
   
   // CloseTradeByTrade ?
   
   if (bb + ss > 0) {
      for (ii=OrdersHistoryTotal()-1; ii>=0; ii--) {
         if (!OrderSelect(ii, SELECT_BY_POS, MODE_HISTORY)) {
            Print("error orderselect");
            
            return;
         }      
         
         if (OrderSymbol() == Symbol() && OrderMagicNumber() == MagicNumber) {
            if (OrderType() == OP_BUY || OrderType() == OP_SELL) { 
               if (OrderCloseTime() > last_bs_tm) {
                  closed ++;
               }
            }
         }
      }      
   }
   
   // first trade
   
   int cmd;
   
   if (bb + ss == 0) {
      GetBuySellSignal(cmd);
      
      if (cmd == OP_BUY) OrderSend(Symbol(), cmd, InitialLots, Ask, 0, 0, 0, string(bb+ss+1)+"-"+string(GetNextEntryLevel(bb+ss)), MagicNumber, 0, clrBlue);
      else if (cmd == OP_SELL) OrderSend(Symbol(), cmd, InitialLots, Bid, 0, 0, 0, string(bb+ss+1)+"-"+string(GetNextEntryLevel(bb+ss)), MagicNumber, 0, clrRed);
   
      return;
   }
   
   // close first trade
   
   if (bb + ss == 1 && ttp > 0) {
      if (OrderSelect(last_bs_tk, SELECT_BY_TICKET)) {
         if (IsOrderClose()) {
            if (!CloseAll()) {
               gb_close_all = true;
            }
            
            return;
         }
      } else return;
   }
   
   // basket close: PlaceStopLoss
   
   if (LossType == PlaceStopLoss && bb + ss >= MaxOrders+1 && StopLossPoint > 0 && last_pt_points <= -StopLossPoint) {
      Print("[Close all due to PlaceStopLoss, last_pt_points=", last_pt_points, ", StopLossPoint=", StopLossPoint, ", Bid=", DoubleToStr(Bid, Digits));
      
      if (!CloseAll()) {
         gb_close_all = true;
      }
      
      return;   
   }
   
   // basket close: PartialLossRetracement
   
   if (LossType == PartialLossRetracement && bb + ss >= MaxOrders+1 && last_pt_points <= -(hh - ll) / Point) {
      Print("[PartialLossRetracement, last_pt_points=", last_pt_points, ", Bid=", DoubleToStr(Bid, Digits));
      
      if (OrderSelect(last_bs_tk, SELECT_BY_TICKET)) {
         OrderClose(OrderTicket(), OrderLots(), OrderClosePrice(), 0, clrYellow);
      }
      
      return;
   }
   
   // basket close: CloseTradeByTrade
   
   if (LossType == CloseTradeByTrade && (bb + ss >= MaxOrders+1 || closed > 0) && last_pt_points <= -(hh - ll) / Point) {
      Print("[CloseTradeByTrade, last_pt_points=", last_pt_points, ", Orders=", bb+ss, ", Closed=", closed, ", Bid=", DoubleToStr(Bid, Digits));
      
      if (OrderSelect(last_bs_tk, SELECT_BY_TICKET)) {
         OrderClose(OrderTicket(), OrderLots(), OrderClosePrice(), 0, clrYellow);
      }
      
      return;
   }
   
   // break event
   
   bool break_even = false;      
   
   if (BreakEvenLevel > 0 && bb + ss > BreakEvenLevel && ttp >= BreakEvenMinProfit) {
      break_even = true;
   }
   
   // basket TP
   
   bool basket_tp = false;
   
   if (bb + ss > 1 && ttp > 0 && RecoveryZoneTPHedgePoints > 0 && last_pt_points >= RecoveryZoneTPHedgePoints) {
      basket_tp = true;
   }
   
   if (bb + ss > 1 && ttp > 0 && RecoveryZoneTPHedgeCurrency > 0 && ttp >= RecoveryZoneTPHedgeCurrency) {
      basket_tp = true;
   }
   
   if ((CloseBasketTPPlusBreakEven && break_even && basket_tp)
      || (!CloseBasketTPPlusBreakEven && (break_even || basket_tp)))
   {
      Print("[CloseBasket, break_even=", break_even, ", basket_tp=", basket_tp, 
         ", last_pt_points=", DoubleToStr(last_pt_points,1), ", be_profit=", DoubleToStr(be_profit,2), ", ttp=", DoubleToStr(ttp,2),
         ", Bid=", DoubleToStr(Bid, Digits));
      
      if (!CloseAll()) { 
         gb_close_all = true;
      }
      
      return;
   }
   
   // CloseBasketAtMaxOrders
   
   if (CloseBasketAtMaxOrders && bb + ss >= MaxOrders) {
      if (OrderSelect(last_bs_tk, SELECT_BY_TICKET)) {
         if ((OrderType() == OP_BUY && Bid <= ll) || (OrderType() == OP_SELL && Bid >= hh)) {
            Print("[CloseBasketAtMaxOrders, Orders=", bb + ss, ", HH=", hh, 
               ", LL=", ll, ", Bid=", DoubleToStr(Bid, Digits));
         
            if (!CloseAll()) {
               gb_close_all = true;
            }
            
            return;
         }
      }
   }
   
   // hedge trade 
   
   if (LossType == CloseTradeByTrade && closed > 0) {
      return;
   }   
   
   double ratio = 0;
   
   if (EnableRecoveryZone && bb + ss + bbp + ssp < MaxOrders+1 && bbp + ssp == 0 && hh > ll) {
      if (CloseBasketAtMaxOrders && bb + ss >= MaxOrders) {
         return;
      }
         
      if (OrderSelect(last_bs_tk, SELECT_BY_TICKET)) {
         if (RecoveryZoneTPHedgePoints > 0) {
            ratio = (hh - ll) / (RecoveryZoneTPHedgePoints * Point) + 1;
         }   
         
         if (RecoveryZoneOrderType == Instant) {
            if (OrderType() == OP_BUY && Bid <= ll) {
               OrderSend(Symbol(), OP_SELL, GetNextLots(bb+ss, lots_sell, ratio), Bid, 0, 0, 0, 
                  string(bb+ss+1)+"-"+string(GetNextEntryLevel(bb+ss)), MagicNumber, 0, clrRed);
               
               return;
            }
            
            if (OrderType() == OP_SELL && Bid >= hh) {
               OrderSend(Symbol(), OP_BUY, GetNextLots(bb+ss, lots_buy, ratio), Ask, 0, 0, 0, 
                  string(bb+ss+1)+"-"+string(GetNextEntryLevel(bb+ss)), MagicNumber, 0, clrBlue);
               
               return;
            }
         }
         
         if (RecoveryZoneOrderType == Pending) {
            if (OrderType() == OP_BUY) {
               OrderSend(Symbol(), OP_SELLSTOP, GetNextLots(bb+ss, lots_sell, ratio), ll, 0, 0, 0, 
                  string(bb+ss+1)+"-"+string(GetNextEntryLevel(bb+ss)), MagicNumber, 0, clrRed);
               
               return;
            }
            
            if (OrderType() == OP_SELL) {
               OrderSend(Symbol(), OP_BUYSTOP, GetNextLots(bb+ss, lots_buy, ratio), hh, 0, 0, 0, 
                  string(bb+ss+1)+"-"+string(GetNextEntryLevel(bb+ss)), MagicNumber, 0, clrBlue);
               
               return;
            }
         }
      }
   }
}

string GetRenkoState()
{
   if (Time[0] != gt_last_renko) {
      gs_renko = "";
      gt_last_renko = Time[0];
      
      if (iCustom(NULL,0,"renko-charts",RenkSizePoints,0,1) < iCustom(NULL,0,"renko-charts",RenkSizePoints,0,2)) gs_renko = "sell";
      else if (iCustom(NULL,0,"renko-charts",RenkSizePoints,0,1) > iCustom(NULL,0,"renko-charts",RenkSizePoints,0,2)) gs_renko = "buy";
   }
   
   return gs_renko;
}

bool IsOrderClose()
{
   if (TakeProfitPoints > 0 && MathAbs(OrderOpenPrice() - OrderClosePrice()) >= TakeProfitPoints * Point) {
      return true;
   }
        
   if (EnablePrimaryHeikenAshiExit) {
      if (OrderType() == OP_BUY && iCustom(NULL,PrimaryHeikenAshiTF,"Heiken Ashi",0,1) > iCustom(NULL,PrimaryHeikenAshiTF,"Heiken Ashi",1,1)) {
         return true;
      }   
      
      if (OrderType() == OP_SELL && iCustom(NULL,PrimaryHeikenAshiTF,"Heiken Ashi",0,1) < iCustom(NULL,PrimaryHeikenAshiTF,"Heiken Ashi",1,1)) {
         return true;
      }
   } 
   
   if (EnableSecondaryHeikenAshiExit) {
      if (OrderType() == OP_BUY && iCustom(NULL,SecondaryHeikenAshiTF,"Heiken Ashi",0,1) > iCustom(NULL,SecondaryHeikenAshiTF,"Heiken Ashi",1,1)) {
         return true;
      }
      
      if (OrderType() == OP_SELL && iCustom(NULL,SecondaryHeikenAshiTF,"Heiken Ashi",0,1) < iCustom(NULL,SecondaryHeikenAshiTF,"Heiken Ashi",1,1)) {
         return true;
      }
   }   
   
   if (EnableRenkoExit && RenkSizePoints > 0) {
      if (OrderType() == OP_BUY && GetRenkoState() == "sell") {
         return true;
      }
      
      if (OrderType() == OP_SELL && GetRenkoState() == "buy") {
         return true;
      }
   }   
         
   return false;
}

bool GetBuySellSignal(int & cmd)
{
   int empty=0, sells=0, buys=0;
   
   cmd = -1;
   
   if (!EnablePrimaryHeikenAshi) empty ++;
   else if (iCustom(NULL,PrimaryHeikenAshiTF,"Heiken Ashi",0,1) > iCustom(NULL,PrimaryHeikenAshiTF,"Heiken Ashi",1,1)) sells ++;
   else if (iCustom(NULL,PrimaryHeikenAshiTF,"Heiken Ashi",0,1) < iCustom(NULL,PrimaryHeikenAshiTF,"Heiken Ashi",1,1)) buys ++;
   
   if (!EnableSecondaryHeikenAshi) empty ++;
   else if (iCustom(NULL,SecondaryHeikenAshiTF,"Heiken Ashi",0,1) > iCustom(NULL,SecondaryHeikenAshiTF,"Heiken Ashi",1,1)) sells ++;
   else if (iCustom(NULL,SecondaryHeikenAshiTF,"Heiken Ashi",0,1) < iCustom(NULL,SecondaryHeikenAshiTF,"Heiken Ashi",1,1)) buys ++;
   
   if (!EnableMa) empty ++;
   else if (Bid < iMA(NULL,MaTF,MaPeriod,MaShift,MaMethod,MaPrice,1)) sells ++;
   else if (Bid > iMA(NULL,MaTF,MaPeriod,MaShift,MaMethod,MaPrice,1)) buys ++;
   
   if (!EnableMaSlope) empty ++;
   else if (iMA(NULL,MaTF,MaPeriod,MaShift,MaMethod,MaPrice,1) < iMA(NULL,MaTF,MaPeriod,MaShift,MaMethod,MaPrice,2) - MaSlopeDeltaPoints * Point) sells ++;
   else if (iMA(NULL,MaTF,MaPeriod,MaShift,MaMethod,MaPrice,1) > iMA(NULL,MaTF,MaPeriod,MaShift,MaMethod,MaPrice,2) + MaSlopeDeltaPoints * Point) buys ++;
   
   if (RenkSizePoints == 0) empty ++;
   else if (GetRenkoState() == "sell") sells ++;
   else if (GetRenkoState() == "buy") buys ++;
   
   if (!EnableMACD) empty ++;
   else if (iMACD(NULL,MACDTF,MACDFastPeriod,MACDSlowPeriod,MACDSignalPeriod,MACDPrice,MODE_MAIN,1) < 0) sells ++;
   else if (iMACD(NULL,MACDTF,MACDFastPeriod,MACDSlowPeriod,MACDSignalPeriod,MACDPrice,MODE_MAIN,1) > 0) buys ++;
   
   if (empty >= 6) {
      return false;
   }   
   
   if (empty + sells >= 6) cmd = OP_SELL;
   else if (empty + buys >= 6) cmd = OP_BUY;
   
   return true;
}

int GetNextEntryLevel(int bbss)
{
   double entry = 0;
   int level;
   
   if (RecoveryZoneType == FixedZone) {
      entry = FixedRecoveryZonePoints;
      
      if (FixedRecoveryZoneAddPoints > 0 && FixedRecoveryZoneAddOrders) {
         level = bbss / FixedRecoveryZoneAddOrders;
         entry += level * FixedRecoveryZoneAddPoints;
      }
      
      if (FixedRecoveryZoneMaxPoints > 0 && entry > FixedRecoveryZoneMaxPoints) entry = FixedRecoveryZoneMaxPoints;
      
      return int(entry);
   } else {
      entry = iHigh(NULL, DynamicRecoveryZoneTF, iHighest(NULL,DynamicRecoveryZoneTF,MODE_HIGH,DynamicRecoveryZoneCandles,0));
      if (entry <= 0) return 0;
      entry -= iLow(NULL, DynamicRecoveryZoneTF, iLowest(NULL,DynamicRecoveryZoneTF,MODE_LOW,DynamicRecoveryZoneCandles,0));
      if (entry <= 0) return 0;
      
      if (DynamicRecoveryZoneDivider > 0) entry /= DynamicRecoveryZoneDivider;
      
      if (entry < DynamicRecoveryZoneMinPoints * Point) entry = DynamicRecoveryZoneMinPoints * Point;
      if (entry > DynamicRecoveryZoneMaxPoints * Point) entry = DynamicRecoveryZoneMaxPoints * Point;
      
      return int(entry / Point);
   }
}

double GetNextLots(int currentbbss, double currentlots, double ratio)
{
   if (RecoveryZoneAddLotsType == Fixed) {
      return InitialLots + currentbbss * FixedAddLots;
   } else if (RecoveryZoneAddLotsType == Custom) {
      string ccc[];
      StringSplit(CustomAddLots, StringGetCharacter(";",0), ccc);
      
      if (ArraySize(ccc) >= currentbbss) {
         return StrToDouble(ccc[currentbbss - 1]);
      } else if (ArraySize(ccc) > 0) {
         return StrToDouble(ccc[ArraySize(ccc) - 1]);
      }
   } else if (RecoveryZoneAddLotsType == Multiple) {
      return NormalizeDouble(InitialLots * MathPow(MultiplierAddLots, currentbbss), 2) - currentlots;
   } else if (RecoveryZoneAddLotsType == Ratio && ratio > 0) {
      return NormalizeDouble(InitialLots * MathPow(ratio, currentbbss), 2) - currentlots;
   }
   
   return InitialLots;
}

bool CloseAll()
{
   for (int ii=OrdersTotal()-1; ii>=0; ii--) {
      if (!OrderSelect(ii, SELECT_BY_POS)) {
         Print("error orderselect");
         
         return false;
      }      
      
      if (OrderSymbol() == Symbol() && OrderMagicNumber() == MagicNumber) {
         if (OrderType() == OP_BUY || OrderType() == OP_SELL) {
            if (!OrderClose(OrderTicket(), OrderLots(), OrderClosePrice(), 0, clrYellow)) {
               return false;
            }   
         } else {
            if (!OrderDelete(OrderTicket())) {
               return false;
            }
         }
      }
   }
   
   return true;      
}