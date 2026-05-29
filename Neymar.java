package meutime;

import robocode.*;
import robocode.util.Utils;
import java.awt.geom.Point2D;
import java.io.IOException;

/**
 * Hunter - Robô agressivo do time.
 * Estratégia: perseguir o inimigo mais próximo, mirar com predição de movimento
 * (linear targeting) e disparar com potência adaptativa.
 * Comunica a posição dos inimigos ao parceiro Bruna.
 */
public class Neymar extends TeamRobot {

    // Alvo atual
    private String targetName = null;
    private double targetX = 0, targetY = 0;
    private double targetVelocity = 0, targetHeading = 0;
    private double targetEnergy = 100;

    // Energia anterior do inimigo (detectar tiros)
    private double lastEnemyEnergy = 100;

    // Contador de scans sem ver o alvo
    private int lostCount = 0;

    @Override
    public void run() {
        // Radar, canhão e corpo giram independentemente
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while (true) {
            if (targetName == null) {
                // Varrer toda a arena até achar alguém
                setTurnRadarRight(360);
            }
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        // Ignorar parceiro
        if (isTeammate(e.getName())) return;

        // Travar o radar no alvo
        double radarTurn = getHeading() + e.getBearing() - getRadarHeading();
        setTurnRadarRight(Utils.normalRelativeAngleDegrees(radarTurn) * 2);

        targetName = e.getName();
        lostCount = 0;

        // Calcular posição absoluta do inimigo
        double angleRad = Math.toRadians(getHeading() + e.getBearing());
        targetX = getX() + e.getDistance() * Math.sin(angleRad);
        targetY = getY() + e.getDistance() * Math.cos(angleRad);
        targetVelocity = e.getVelocity();
        targetHeading = e.getHeadingRadians();

        // Detectar se inimigo atirou (queda de energia)
        if (lastEnemyEnergy - e.getEnergy() >= 0.1 && lastEnemyEnergy - e.getEnergy() <= 3.1) {
            // Inimigo provavelmente atirou — desviar perpendicular
            double perp = getHeadingRadians() + Math.PI / 2;
            setTurnRight(Utils.normalRelativeAngleDegrees(
                    Math.toDegrees(perp) - getHeading()));
            setAhead(100);
        }
        lastEnemyEnergy = e.getEnergy();
        targetEnergy = e.getEnergy();

        // ---- Mira com predição linear ----
        firePredictive(e.getDistance());

        // ---- Movimento: perseguir o inimigo ----
        double bearing = e.getBearing();
        // Manter distância ideal de 150px
        if (e.getDistance() > 200) {
            setTurnRight(bearing);
            setAhead(e.getDistance() - 150);
        } else if (e.getDistance() < 100) {
            setTurnRight(bearing);
            setBack(100);
        } else {
            // Circular strafing
            setTurnRight(bearing + 90);
            setAhead(80);
        }

        // Comunicar posição ao Bruna
        try {
            broadcastMessage(new double[]{targetX, targetY, e.getDistance()});
        } catch (IOException ex) {
            // ignora erro de comunicação
        }
    }

    /**
     * Mira preditiva: estima onde o inimigo vai estar quando o tiro chegar.
     */
    private void firePredictive(double distance) {
        // Potência adaptativa: mais longe = tiro mais fraco (mais rápido)
        double power;
        if (distance < 150) power = 3.0;
        else if (distance < 300) power = 2.0;
        else power = 1.0;

        double bulletSpeed = 20 - 3 * power;

        // Tempo estimado até o tiro chegar
        double t = distance / bulletSpeed;

        // Posição futura do inimigo (movimento linear)
        double futureX = targetX + Math.sin(targetHeading) * targetVelocity * t;
        double futureY = targetY + Math.cos(targetHeading) * targetVelocity * t;

        // Ângulo absoluto até a posição futura
        double dx = futureX - getX();
        double dy = futureY - getY();
        double aimAngle = Math.toDegrees(Math.atan2(dx, dy));

        double gunTurn = Utils.normalRelativeAngleDegrees(aimAngle - getGunHeading());
        setTurnGunRight(gunTurn);

        // Atirar se o canhão estiver próximo do alvo e tiver energia suficiente
        if (Math.abs(gunTurn) < 5 && getEnergy() > power) {
            setFire(power);
        }
    }

    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        if (e.getName().equals(targetName)) {
            targetName = null;
            lastEnemyEnergy = 100;
            // Varrer para achar novo alvo
            setTurnRadarRight(360);
        }
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        // Recuar e virar ao bater na parede
        setBack(50);
        setTurnRight(45);
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        // Desviar perpendicular ao tiro recebido
        setTurnRight(e.getBearing() + 90);
        setAhead(100);
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        if (!isTeammate(e.getName())) {
            setFire(3);
            setBack(80);
        }
    }

    @Override
    public void onMessageReceived(MessageEvent e) {
        // Receber posição de inimigo do Bruna (caso Bruna veja alguém que Hunter não vê)
        if (e.getMessage() instanceof double[]) {
            double[] pos = (double[]) e.getMessage();
            if (targetName == null && pos.length >= 2) {
                // Girar radar para a direção indicada pelo parceiro
                double angleToTarget = Math.toDegrees(
                        Math.atan2(pos[0] - getX(), pos[1] - getY()));
                double radarTurn = Utils.normalRelativeAngleDegrees(
                        angleToTarget - getRadarHeading());
                setTurnRadarRight(radarTurn);
            }
        }
    }

    @Override
    public void onDeath(DeathEvent e) {
        // Nada a fazer
    }
}
