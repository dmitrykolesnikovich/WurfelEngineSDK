/*
 * Copyright 2015 Benedikt Vogler.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * If this software is used for a game the official „Wurfel Engine“ logo or its name must be
 *   visible in an intro screen or main menu.
 * * Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * * Neither the name of Benedikt Vogler nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.bombinggames.wurfelengine;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.bombinggames.wurfelengine.core.AbstractMainMenu;
import com.bombinggames.wurfelengine.core.Controller;
import com.bombinggames.wurfelengine.core.EngineView;
import com.bombinggames.wurfelengine.core.GameView;
import com.bombinggames.wurfelengine.core.GameplayScreen;
import com.bombinggames.wurfelengine.core.WEScreen;
import com.bombinggames.wurfelengine.core.WorkingDirectory;
import com.bombinggames.wurfelengine.core.console.Console;
import com.bombinggames.wurfelengine.core.cvar.CVarSystemRoot;
import com.bombinggames.wurfelengine.core.gameobjects.AbstractEntity;
import com.bombinggames.wurfelengine.core.loading.LoadingScreen;
import com.bombinggames.wurfelengine.extension.basicmainmenu.BasicMainMenu;
import com.bombinggames.wurfelengine.soundengine.SoundEngine;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 * The main class of the engine. To create a new engine use
 * {@link WE#launch(String, String[])} The Wurfel Engine needs Java &gt;= v1.8
 * and the API libGDX v1.8.0 (may work with older versions).
 *
 * @author Benedikt S. Vogler
 * @version 1.7.6
 */
public class WE {

	/**
	 * The version of the Engine
	 */
	public static final String VERSION = "1.7.6";
	/**
	 * the working directory where the config and files are saved
	 */
	private static final File WORKDIR = WorkingDirectory.getWorkingDirectory();

	/**
	 * The CVar system used by the engine.
	 */
	private final static CVarSystemRoot CVARS = new CVarSystemRoot(new File(WORKDIR + "/engine.wecvars"));
	/**
	 * The sound engine managing the sfx.
	 */
	public static final SoundEngine SOUND = new SoundEngine();
	private static final WEGame GAME = new WEGame();
	private static final AssetManager ASSETMANAGER = new AssetManager();
	private static final LwjglApplicationConfiguration CONFIG = new LwjglApplicationConfiguration();
	private static final ArrayList<LaunchCommand> POSTLAUNCHCOMMANDS = new ArrayList<>(0);
	private static GameplayScreen gameplayScreen;
	private static AbstractMainMenu mainMenu;
	private static Console console;
	private static EngineView engineView;
	private static LwjglApplication application;
	private static boolean skipintro = false;
	private static String iconPath = null;
	/**
	 * default is that every manager state is game.
	 */
	private static boolean inGame = false;
	private static boolean inEditor = false;

	/**
	 * Pass the mainMenu which gets displayed when you call launch().
	 *
	 * @param mainMenu
	 * @see WE#launch(java.lang.String, java.lang.String[])
	 */
	public static void setMainMenu(final AbstractMainMenu mainMenu) {
		WE.mainMenu = mainMenu;
	}

	/**
	 * Set a screen as active.
	 *
	 * @param screen
	 */
	public static void setScreen(WEScreen screen) {
		engineView.resetInputProcessors();
		GAME.setScreen(screen);
	}

	/**
	 * Add commands which will be executed after the engine launched.
	 *
	 * @param command
	 */
	public static void addPostLaunchCommands(LaunchCommand command) {
		POSTLAUNCHCOMMANDS.add(command);
	}

	/**
	 *
	 * @param internalPath
	 */
	public static void addIcon(String internalPath) {
		iconPath = internalPath;
	}

	/**
	 * Start the engine. You should have passed a main menu first.<br> Until the
	 * engine is launched it can take a while. Code that can only be run after
	 * the engine has openend should be run in the screen class.
	 *
	 * @param title The title, which is displayed in the window.
	 * @param args Wurfel Engine launch parameters. For a list look in the wiki.
	 * @see #setMainMenu(com.bombinggames.wurfelengine.core.AbstractMainMenu)
	 */
	public static void launch(final String title, final String[] args) {
		CONFIG.resizable = false;
		//config.setFromDisplayMode(LwjglApplicationConfiguration.getDesktopDisplayMode());
		CONFIG.fullscreen = true;
		CONFIG.vSyncEnabled = false;//if set to true the FPS is locked to 60
		
		//get current resolution
		DisplayMode dpms = LwjglApplicationConfiguration.getDesktopDisplayMode();
		CONFIG.width = dpms.width;
		CONFIG.height = dpms.height;
		
		//arguments
		if (args != null && args.length > 0) {
			//look if contains launch parameters
			for (int i = 0; i < args.length; i++) {
				switch (args[i]) {
					case "-fullscreen":
					case "-f":
						//start in fullscreen
						CONFIG.fullscreen = true;
						break;
					case "-windowed":
						//start in windowed mode
						CONFIG.fullscreen = false;
						break;
					case "-w":
						i++;
						if (i < args.length){
							//set the width
							CONFIG.width = Integer.parseInt(args[i]);
						} else {
							System.err.println("missing width after launch parameter");
						}
						break;
					case "-h":
						i++;
						if (i < args.length){
							//set the height
							CONFIG.height = Integer.parseInt(args[i]);
						} else {
							System.err.println("missing height after launch parameter");
						}
						break;
					case "-skipintro":
						skipintro = true;
						break;
					case "-v":
						System.out.println(WE.VERSION);
						System.exit(0);
						return;
					default:
						System.out.println("Unknown launch parameter " + args[i]);
				}
			}
		}

		//load cvars
		CVARS.load();

		// set the name of the application menu item on mac
		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", title);
		}

		CONFIG.foregroundFPS = CVARS.getValueI("limitFPS");//don't lock FPS
		CONFIG.backgroundFPS = 60;//60 FPS in background
		//config.addIcon("com/BombingGames/caveland/icon.png", Files.FileType.Internal); //commented this line because on mac this get's overwritten by something during runtime. mac build is best made via native packaging
		if (iconPath != null) {
			CONFIG.addIcon(iconPath, Files.FileType.Internal);//windows and linux?
		}

		//load saved resolution
		int width = CVARS.getValueI("resolutionx");
		if (width > 0 && CONFIG.width <= 640) {
			CONFIG.width = width;
		}

		int height = CVARS.getValueI("resolutiony");
		if (height > 0 && CONFIG.height <= 480) {
			CONFIG.height = CVARS.getValueI("resolutiony");
		}

		//limit resolution to maximum
		if (CONFIG.width > dpms.width) {
			CONFIG.width = dpms.width;
		}

		if (CONFIG.height > dpms.height) {
			CONFIG.height = dpms.height;
		}

		CONFIG.title = title + " " + CONFIG.width + "x" + CONFIG.height;

		//register entitys
		AbstractEntity.registerEngineEntities();

		System.out.println("Fire Engine…");
		application = new LwjglApplication(GAME, CONFIG);
		application.setLogLevel(Application.LOG_DEBUG);
	}

	/**
	 * returns the pointer to the LWJGL configuration of the game window.
	 *
	 * @return
	 */
	public static LwjglApplicationConfiguration getLwjglApplicationConfiguration() {
		return CONFIG;
	}

	/**
	 * Initialize the main game with you custom controller and view. This call
	 * shows the loadingScreen and disposes the menu.
	 *
	 * @param controller
	 * @param view
	 * @param customLoadingScreen
	 * @see com.bombinggames.wurfelengine.WE#startGame()
	 */
	public static void initAndStartGame(final Controller controller, final GameView view, LoadingScreen customLoadingScreen) {
		if (GAME != null) {
			Gdx.app.log("Wurfel Engine", "Initializing game using Controller:" + controller.toString());
			Gdx.app.log("Wurfel Engine", "and View:" + view.toString());
			Gdx.app.log("Wurfel Engine", "and Config:" + CONFIG.toString());

			getEngineView().getEditorToggler().setGameplayManagers(
				view
			);

			if (gameplayScreen != null) {
				gameplayScreen.dispose();//remove gameplayscreen if it already exists
			}
			gameplayScreen = new GameplayScreen(
				controller,
				view,
				customLoadingScreen
			);
			getConsole().setGameplayRef(gameplayScreen);
			mainMenu.dispose();
		} else {
			Gdx.app.error("Wurfel Engine", "You must construct a WE instance first before calling initGame.");
		}
	}

	/**
	 * Use different controller and views, which reinitializes them. Enter the
	 * editor via {@link #startEditor()}
	 *
	 * @param controller the new controller
	 * @param view the new view
	 */
	public static void switchSetupWithInit(final Controller controller, final GameView view) {
		Gdx.app.debug("Wurfel Engine", "Switching setup and ReInit using Controller:" + controller.toString());
		Gdx.app.debug("Wurfel Engine", "and View:" + view.toString());
		inGame = true;
		inEditor = false;
		getEngineView().getEditorToggler().setGameplayManagers(
			view
		);
		engineView.resetInputProcessors();
		gameplayScreen.getController().exit();
		gameplayScreen.getView().exit();
		gameplayScreen.setController(controller);

		//initialize
		Controller.getMap().disposeEntities();
		controller.init();
		view.init(controller, gameplayScreen.getView());

		gameplayScreen.setView(view);
		//enter
		controller.enter();
		view.enter();
	}

	/**
	 *
	 * @param controller
	 * @param view
	 */
	public static void switchSetup(final Controller controller, final GameView view) {
		switchSetup(controller, view, false);
	}

	/**
	 * Continue to use already initialized controller and view. Enter the editor
	 * via {@link #startEditor(boolean)}
	 *
	 * @param controller the new controller
	 * @param view the new view
	 * @param editor true if this is the editor setup
	 */
	private static void switchSetup(final Controller controller, final GameView view, final boolean editor) {
		Gdx.app.debug("Wurfel Engine", "Switching setup using Controller: " + controller.toString());
		Gdx.app.debug("Wurfel Engine", "and View: " + view.toString());
		inGame = true;
		inEditor = editor;
		if (!inEditor) {
			getEngineView().getEditorToggler().setGameplayManagers(
				view
			);
		}
		engineView.resetInputProcessors();
		gameplayScreen.getController().exit();
		gameplayScreen.getView().exit();
		gameplayScreen.setController(controller);

		//init if not initialized
		if (!controller.isInitalized()) {
			controller.init();
		}
		if (!view.isInitalized()) {
			view.init(controller, gameplayScreen.getView());
		}

		gameplayScreen.setView(view);
		//enter
		view.enter();
		controller.enter();
	}

	/**
	 *
	 * @param view
	 * @param editor true if this is the editor setup
	 */
	public static void switchView(final GameView view, boolean editor) {
		Gdx.app.debug("Wurfel Engine", "View switch: " + view.toString());
		inEditor = editor;
		if (!inEditor) {
			getEngineView().getEditorToggler().setGameplayManagers(
				view
			);
		}
		engineView.resetInputProcessors();
		gameplayScreen.getView().exit();

		//init if not initialized
		if (!view.isInitalized()) {
			view.init(gameplayScreen.getController(), gameplayScreen.getView());
		}
		gameplayScreen.setView(view);
		//enter
		view.enter();
	}

	/**
	 * Switch into the map editor. If the editor is already running it will
	 * restart it.
	 *
	 */
	public static void startEditor() {
		switchView(gameplayScreen.getEditorView(), true);
	}

	/**
	 *
	 * @return
	 */
	public static boolean isInEditor() {
		return inEditor;
	}

	/**
	 * Starts the actual game using the custom gameplayScreen. This is called
	 * after the loading screen.
	 */
	public static void startGame() {
		SOUND.disposeMusic();
		Gdx.app.log("Wurfel Engine", "Starting the gameplay…");
		GAME.setScreen(gameplayScreen);
		inGame = true;
		inEditor = false;
	}

	/**
	 *
	 * @return true when in gameplay (editor counts as gameplay)
	 */
	public static boolean isInGameplay() {
		return inGame;
	}

	/**
	 * Starts the actual game using the gameplayScreen you initialized with
	 * <i>initGame(Controller controller, GameView view)</i>. This is called
	 * after the loading screen.
	 */
	public static void showMainMenu() {
		if (gameplayScreen != null) {
			gameplayScreen.dispose();
		}
		inEditor = false;
		inGame = false;
		gameplayScreen = null;
		engineView.resetInputProcessors();
		GAME.setScreen(mainMenu);
	}

	/**
	 * Get the credits of the engine.
	 *
	 * @return a long string with breaks
	 */
	public final static String getCredits() {
		String newline = System.getProperty("line.separator");
		return "Wurfel Engine (" + VERSION + ")" + newline + newline
			+ "Created by:" + newline
			+ "Benedikt S. Vogler" + newline + newline
			+ "Quality Assurance:" + newline
			+ "Thomas Vogt" + newline + newline
			+ "Wurfel Engine uses libGDX." + newline;
	}

	/**
	 * Returns the save file folder, wich is different on every OS.
	 *
	 * @return a folder
	 */
	public static File getWorkingDirectory() {
		return WORKDIR;
	}

	/**
	 * You can switch to fullscreen. It only works if the current window
	 * resolution is supported by your hardware.
	 *
	 * @param fullscreen
	 */
	public static void setFullscreen(final boolean fullscreen) {
		DisplayMode currentMode = Gdx.graphics.getDisplayMode();
		if (fullscreen)
			Gdx.graphics.setFullscreenMode(currentMode);
		else {
			Gdx.graphics.setWindowedMode(currentMode.width, currentMode.height);
		}
		CONFIG.fullscreen = Gdx.graphics.isFullscreen();
		Gdx.app.debug("Wurfel Engine", "Set to fullscreen:" + fullscreen + " It is now:" + Gdx.graphics.isFullscreen());
	}

	/**
	 * Get an asset from the asset manager
	 *
	 * @param <T>
	 * @param filename the name of the file
	 * @return returns the asset
	 * @throws java.io.FileNotFoundException
	 */
	public static <T> T getAsset(String filename) throws FileNotFoundException {
		try {
			return ASSETMANAGER.get(filename);
		} catch (com.badlogic.gdx.utils.GdxRuntimeException ex){
			throw new FileNotFoundException("Asset \"" + filename + "\" could not be retrieved because it is not loaded.");
		}
	}

	/**
	 * To load assets you can use getAsset(String filename)
	 *
	 * @return the asset manager.
	 */
	public static AssetManager getAssetManager() {
		if (GAME != null) {
			return ASSETMANAGER;
		} else {
			Gdx.app.error("Wurfel Engine", "There is no instance of the engine. You should call initGame first.");
			return null;
		}
	}

	/**
	 * Returns the Console. Use {@link com.bombinggames.wurfelengine.core.console.Console#add(java.lang.String)
	 * }to add messages to the console.
	 *
	 * @return The console.
	 */
	public static Console getConsole() {
		if (console == null) {
			System.err.println("Engine not running yet.");
		}
		return console;
	}

	/**
	 *
	 * @return Get the view independent view.
	 */
	public static EngineView getEngineView() {
		return engineView;
	}

	/**
	 * updates and render the global things e.g. the console
	 *
	 * @param dt time in ms
	 */
	public static void updateAndRender(float dt) {
		console.update(dt);
		engineView.update(dt);
		SOUND.update(dt);

		engineView.getStage().act(dt);
		engineView.getStage().draw();
	}

	/**
	 * should rarely used because allows global access
	 *
	 * @return
	 */
	public static GameplayScreen getGameplay() {
		return gameplayScreen;
	}
	
	/**
	 *
	 * @return
	 */
	public static CVarSystemRoot getCVars() {
		return CVARS;
	}

	private static class WurfelEngineIntro extends WEScreen {

		private final Sprite lettering;
		private final SpriteBatch batch;
		private float alpha = 0;
		private boolean increase = true;
		private final Sound startupsound;
		private final Interpolation interpolate;

		WurfelEngineIntro() {
			batch = new SpriteBatch();
			lettering = new Sprite(new Texture(Gdx.files.internal("com/bombinggames/wurfelengine/lettering.png")));
			lettering.setX((Gdx.graphics.getWidth() - lettering.getWidth()) / 2);
			lettering.setY((Gdx.graphics.getHeight() - lettering.getHeight()) / 2);
			startupsound = Gdx.audio.newSound(Gdx.files.internal("com/bombinggames/wurfelengine/soundengine/sounds/startup.mp3"));
			startupsound.play();
			interpolate = new Interpolation.ExpOut(2, 7);
			//lettering.flip(false, true);
		}

		@Override
		public void renderImpl(float dt) {
			if (increase) {
				if (alpha >= 1) {
					alpha = 1;
					increase = false;
				} else {
					alpha += dt / 1500f;
				}
				drawLettering();
			} else if (alpha <= 0) {
				alpha = 0;
				dispose();
			} else {
				alpha -= dt / 1000f;
				drawLettering();
			}
		}

		void drawLettering() {
			lettering.setColor(1f, 1f, 1f, interpolate.apply(alpha));

			//clear & set background to black
			Gdx.gl20.glClearColor(0f, 0f, 0f, 1f);
			Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

			batch.begin();
			lettering.draw(batch);
			batch.end();
		}

		@Override
		public void resize(int width, int height) {
		}

		@Override
		public void show() {
			Gdx.app.debug("Intro", "Showing intro…");
		}

		@Override
		public void hide() {
		}

		@Override
		public void pause() {
		}

		@Override
		public void resume() {
		}

		@Override
		public void dispose() {
			startupsound.dispose();
			WE.showMainMenu();
		}
	}

	private static class WEGame extends Game {

		@Override
		public void create() {
			// get the current display mode of the monitor the window is on
			DisplayMode mode = Gdx.graphics.getDisplayMode();
			// set the window to fullscreen mode
			//Gdx.graphics.setFullscreenMode(mode);
			Gdx.graphics.getBackBufferWidth();
			if (!skipintro) {
				GAME.setScreen(new WurfelEngineIntro());
			}

			if (mainMenu == null) {
				Gdx.app.error("WEMain", "No main menu object could be found. Pass one with 'setMainMenu()' before launching.");
				Gdx.app.error("WEMain", "Using a predefined BasicMainMenu.");
				mainMenu = new BasicMainMenu();
			}
			engineView = new EngineView(null, null);

			console = new Console(
				engineView.getSkin(),
				50,
				Gdx.graphics.getHeight() - 700
			);

			Gdx.app.debug("WE", "Initializing main menu...");
			mainMenu.init();

			if (skipintro) {
				setScreen(mainMenu);
			}

			POSTLAUNCHCOMMANDS.forEach(a -> {
				a.perform();
			});
		}

		@Override
		public void dispose() {
			super.dispose();
			CVARS.dispose();
			SOUND.dispose();
		}

	}

}
